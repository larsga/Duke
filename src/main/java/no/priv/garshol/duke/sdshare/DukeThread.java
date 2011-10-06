
package no.priv.garshol.duke.sdshare;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collection;

import org.apache.lucene.index.CorruptIndexException;

import no.priv.garshol.duke.Logger;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.DataSource;
import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.PrintMatchListener;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.JDBCLinkDatabase;
import no.priv.garshol.duke.LinkDatabaseMatchListener;
import no.priv.garshol.duke.utils.ObjectUtils;

/**
 * A thread which can be run inside an application server to set up
 * Duke to poll against a datasource for incoming records to process.
 */
public class DukeThread {
  private String configfile;
  private String linkjdbcuri;
  private String driverklass;
  private String dbtype;
  private String tblprefix;
  private Logger logger;
  private int batch_size;
  private int sleep_interval;
  private int check_interval;

  private boolean stopped;
  private long lastCheck;  // time we last checked
  private long lastRecord; // most recent time we saw a new record
  private String status;   // what's up?
  private int records;     // number of records processed

  private Thread thread;
  private Configuration config;
  private JDBCLinkDatabase linkdb;
  private Properties jdbcprops;
  private Processor processor;

  public DukeThread(Properties props) {
    this.configfile = props.getProperty("duke.configfile");
    this.linkjdbcuri = props.getProperty("duke.linkjdbcuri");
    this.driverklass = props.getProperty("duke.jdbcdriver");
    this.dbtype = props.getProperty("duke.database");
    this.tblprefix = props.getProperty("duke.table-prefix");
    this.status = "Instantiated, not running";
    this.batch_size = 40000;
    this.sleep_interval = 100;
    this.stopped = true;

    this.jdbcprops = new Properties();
    if (props.getProperty("duke.username") != null)
      jdbcprops.put("user", props.getProperty("duke.username"));
    if (props.getProperty("duke.password") != null)
      jdbcprops.put("password", props.getProperty("duke.password"));
    if (props.getProperty("duke.check-interval") != null)
      this.check_interval = Integer.parseInt(props.getProperty("duke.check-interval"));
    else
      this.check_interval = 50000;

    String loggerclass = props.getProperty("duke.logger-class");
    if (loggerclass != null)
      logger = (Logger) ObjectUtils.instantiate(loggerclass);
  }

  public void init() {
    if (logger != null)
      logger.info("Initializing duke-ui");
    try {
      config = ConfigLoader.load(configfile);
      processor = new Processor(config, false);
      if (logger != null)
        processor.setLogger(logger);
      //processor.addMatchListener(new PrintMatchListener(true, true));

      linkdb = new JDBCLinkDatabase(driverklass, linkjdbcuri, dbtype,
                                    jdbcprops);
      if (tblprefix != null)
        linkdb.setTablePrefix(tblprefix);
      processor.addMatchListener(new LinkDatabaseMatchListener(config, linkdb));
    } catch (Throwable e) {
      // this means init failed, and we need to clean up so that we can try
      // again later. unfortunately, we don't know what failed, so we need
      // to be careful
      config = null;
      if (processor != null)
        try {
          processor.close();
        } catch (Exception e2) {
          // FIXME: log
        }
      if (linkdb != null)
        linkdb.close();

      throw new RuntimeException(e); // we failed, so signal that
    }
  }

  public void start() {
    if (logger != null)
      logger.info("Starting duke-ui thread");
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

    while (!stopped) {
      status = "Processing";
      int count = 0;
      Collection<Record> batch = new ArrayList();

      for (DataSource source : config.getDataSources()) {
        RecordIterator it = source.getRecords();
        lastCheck = System.currentTimeMillis();
        while (it.hasNext() && !stopped) {
          lastRecord = System.currentTimeMillis();
          Record record = it.next();
          batch.add(record);
          count++;
          records++;
          if (count % batch_size == 0) {
            processor.deduplicate(batch);
            linkdb.commit();
            it.batchProcessed();
            batch = new ArrayList();
          }
        }

        if (!batch.isEmpty()) {
          processor.deduplicate(batch);
          linkdb.commit();
          it.batchProcessed();
          batch = new ArrayList();
        }
        
        it.close();
        if (stopped)
          break;
      }

      // waiting check_interval ms, while taking sleep_interval ms
      // long naps so we can break off faster if the server is shut
      // down
      long wait_start = System.currentTimeMillis();
      do {
        try {
          status = "Sleeping";
          Thread.sleep(sleep_interval);
        } catch (InterruptedException e) {
          // well, so what?
        }
      } while (!stopped &&
               (System.currentTimeMillis() - wait_start) < check_interval);
    }

    status = "Thread stopped";
    stopped = true;
  }

  public void pause() {
    if (logger != null)
      logger.info("Pausing duke-ui");
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
    if (logger != null)
      logger.info("Closing duke-ui");
    stopped = true;
    status = "Closed";
    try {
      if (processor != null)
        processor.close();
      if (linkdb != null)
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