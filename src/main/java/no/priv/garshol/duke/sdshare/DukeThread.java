
package no.priv.garshol.duke.sdshare;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Collection;

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
public class DukeThread extends Thread {
  private String configfile;
  private String linkjdbcuri;
  private int batch_size;

  private boolean stopped;
  private long lastCheck;  // time we last checked
  private long lastRecord; // most recent time we saw a new record
  private String status;   // what's up?
  private int records;     // number of records processed  

  public DukeThread(String configfile, String linkjdbcuri) {
    this.configfile = configfile;
    this.linkjdbcuri = linkjdbcuri;
    this.status = "Instantiated, not running";
    this.batch_size = 40000;
  }

  public void run() {
    status = "Thread running";
    try {
      run_();
    } catch (Throwable e) {
      status = "Thread stopped on error: " + e;
      stopped = true;
      throw new RuntimeException(e);
    }
  }
  
  public void run_() {
    Configuration config = ConfigLoader.load(configfile); 
    Database database = config.getDatabase();
    //database.addMatchListener(new PrintMatchListener(true, true));

    Properties props = new Properties();
    LinkDatabase linkdb = new JDBCLinkDatabase("org.h2.Driver", linkjdbcuri,
                                               props);
    database.addMatchListener(new LinkDatabaseMatchListener(database, linkdb));
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
        Thread.sleep(10000); // FIXME: configurable interval?
      } catch (InterruptedException e) {
        // well, so what?
      }
    }

    status = "Thread stopped";
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
}