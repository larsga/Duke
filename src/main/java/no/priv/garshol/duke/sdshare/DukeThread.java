
package no.priv.garshol.duke.sdshare;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collection;

import org.apache.lucene.index.CorruptIndexException;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.DataSource;
import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Deduplicator;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.PrintMatchListener;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.JDBCLinkDatabase;
import no.priv.garshol.duke.LinkDatabaseMatchListener;

/**
 * A thread which can be run inside an application server to set up
 * Duke to poll against a datasource for incoming records to process.
 */
public class DukeThread {
  private String configfile;
  private String linkjdbcuri;
  private int batch_size;
  private int sleep_interval;

  private boolean stopped;
  private long lastCheck;  // time we last checked
  private long lastRecord; // most recent time we saw a new record
  private String status;   // what's up?
  private int records;     // number of records processed

  private Thread thread;
  private Configuration config;
  private Database database;
  private JDBCLinkDatabase linkdb;

  public DukeThread(String configfile, String linkjdbcuri) {
    this.configfile = configfile;
    this.linkjdbcuri = linkjdbcuri;
    this.status = "Instantiated, not running";
    this.batch_size = 40000;
    this.sleep_interval = 10000;
    this.stopped = true;
  }

  public void init() {
    try {
      config = ConfigLoader.load(configfile); 
      database = config.getDatabase(false);
      //database.addMatchListener(new PrintMatchListener(true, true));

      Properties props = new Properties();
      linkdb = new JDBCLinkDatabase("org.h2.Driver", linkjdbcuri, props);
      database.addMatchListener(new LinkDatabaseMatchListener(database, linkdb));
    } catch (Throwable e) {
      // this means init failed, and we need to clean up so that we can try
      // again later. unfortunately, we don't know what failed, so we need
      // to be careful
      config = null;
      if (database != null)
        try {
          database.close();
        } catch (Exception e2) {
          // FIXME: log
        }
      if (linkdb != null)
        linkdb.close();

      throw new RuntimeException(e); // we failed, so signal that
    }
  }

  public void start() {
    thread = new RealThread(this);
    thread.start();
  }
  
  public void run() {
    status = "Thread running";
    stopped = false;
    try {
      run_();
    } catch (Throwable e) {
      status = "Thread stopped on error: " + e;
      stopped = true;
      throw new RuntimeException(e);
    }
  }
  
  public void run_() {
    if (config == null) 
      init();
    Deduplicator dedup = new Deduplicator(database);

    while (!stopped) {
      status = "Processing";
      int count = 0;
      Collection<Record> batch = new ArrayList();

      for (DataSource source : config.getDataSources()) {
        RecordIterator it = source.getRecords();
        lastCheck = System.currentTimeMillis();
        while (it.hasNext()) {
          lastRecord = System.currentTimeMillis();
          Record record = it.next();
          batch.add(record);
          count++;
          records++;
          if (count % batch_size == 0) {
            dedup.process(batch);
            linkdb.commit();
            batch = new ArrayList();
          }
        }
        it.close();
      }

      if (!batch.isEmpty()) {
        dedup.process(batch);
        linkdb.commit();
      }

      try {
        status = "Sleeping";
        Thread.sleep(sleep_interval);
      } catch (InterruptedException e) {
        // well, so what?
      }
    }

    status = "Thread stopped";
    stopped = true;
  }

  public void pause() {
    stopped = true;
    status = "Waiting for thread to stop";
  }

  public long getLastRecord() {
    return lastRecord;
  }

  public long getLastCheck() {
    return lastCheck;
  }

  public String getStatus() {
    return status;
  }

  public boolean getStopped() {
    return stopped;
  }

  public int getRecords() {
    return records;
  }

  public void setBatchSize(int batch_size) {
    this.batch_size = batch_size;
  }

  public void setSleepInterval(int sleep_interval) {
    this.sleep_interval = sleep_interval;
  }

  public void close() {
    stopped = true;
    status = "Closed";
    try {
      database.close();
      linkdb.close();
    } catch (CorruptIndexException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The actual thread class. It's separated out from the main class
   * so that the main class can hold the state, and we can create and
   * destroy threads without having to reconnect to the index/db etc.
   */
  static class RealThread extends Thread {
    private DukeThread duke;

    public RealThread(DukeThread duke) {
      this.duke = duke;
    }
    
    public void run() {
      duke.run();
    }
  }
}