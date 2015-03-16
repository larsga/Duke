
package no.priv.garshol.duke.server;

import static no.priv.garshol.duke.utils.PropertyUtils.get;

import java.io.IOException;
import java.util.Properties;

import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.JDBCLinkDatabase;
import no.priv.garshol.duke.JNDILinkDatabase;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.Logger;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.RDBMSLinkDatabase;
import no.priv.garshol.duke.matchers.AbstractMatchListener;
import no.priv.garshol.duke.matchers.LinkDatabaseMatchListener;
import no.priv.garshol.duke.utils.ObjectUtils;
// we use this to make it easier to deal with properties

/**
 * The central class that receives notifications from the UI and timer
 * threads, controlling the actual work performed.
 */
public class DukeController extends AbstractMatchListener {
  private String status;   // what's up?
  private int records;     // number of records processed
  private int batch_size;  // batch size in Duke
  private long lastCheck;  // time we last checked
  private long lastRecord; // most recent time we saw a new record
  private int error_factor;// how many times to skip processing on errors
  /**
   * When processing fails with an error, this variable is set to some
   * n, which is the number of processing() calls to skip before we
   * try again. This implements longer check delays when errors occur.
   */
  private int error_skips;
  /**
   * Size of the last batch we saw.
   */
  private int last_batch_size;

  private Processor processor;
  private LinkDatabase linkdb;
  private Logger logger;
  
  public DukeController(Properties props) {
    this.status = "Initialized, inactive";
    String configfile = get(props, "duke.configfile");
    
    try {
      // setting up logger
      String loggerclass = get(props, "duke.logger-class", null);
      if (loggerclass != null) {
        logger = (Logger) ObjectUtils.instantiate(loggerclass);
        logger.debug("DukeController starting up");
      }

      // loading configuration
      Configuration config = ConfigLoader.load(configfile); 
      this.processor = new Processor(config, false);
      this.linkdb = makeLinkDatabase(props);
      processor.addMatchListener(new LinkDatabaseMatchListener(config, linkdb));
      processor.addMatchListener(this);
      batch_size = get(props, "duke.batch-size", 40000);
      error_factor = get(props, "duke.error-wait-skips", 6);

      // add loggers
      if (logger != null) {
        processor.setLogger(logger);
        if (linkdb instanceof RDBMSLinkDatabase)
          ((RDBMSLinkDatabase) linkdb).setLogger(logger);
      }
    } catch (Throwable e) {
      // this means init failed, and we need to clean up so that we can try
      // again later. unfortunately, we don't know what failed, so we need
      // to be careful
      if (processor != null)
        try {
          processor.close();
        } catch (Exception e2) {
          if (logger != null)
            logger.error("Couldn't close processor", e2);
        }
      if (linkdb != null)
        linkdb.close();

      throw new DukeException(e); // we failed, so signal that
    }
  }

  /**
   * Runs the record linkage process.
   */
  public void process() {
    // are we ready to process yet, or have we had an error, and are
    // waiting a bit longer in the hope that it will resolve itself?
    if (error_skips > 0) {
      error_skips--;
      return;
    }
    
    try {
      if (logger != null)
        logger.debug("Starting processing");
      status = "Processing";
      lastCheck = System.currentTimeMillis();

      // FIXME: how to break off processing if we don't want to keep going?
      processor.deduplicate(batch_size);

      status = "Sleeping";
      if (logger != null)
        logger.debug("Finished processing");
    } catch (Throwable e) {
      status = "Thread blocked on error: " + e;
      if (logger != null)
        logger.error("Error in processing; waiting", e);
      error_skips = error_factor;
    }
  }

  /**
   * Shuts down the controller, releasing all resources.
   */
  public void close() throws IOException {
    processor.close();
    linkdb.close();
  }

  public String getStatus() {
    return status;
  }

  public boolean isErrorBlocked() {
    return error_skips > 0 || status.startsWith("Thread blocked");
  }

  public long getLastCheck() {
    return lastCheck;
  }

  public long getLastRecord() {
    return lastRecord;
  }

  public int getRecordCount() {
    return records;
  }

  // called by timer thread
  void reportError(Throwable throwable) {
    if (logger != null)
      logger.error("Timer reported error", throwable);
    status = "Thread blocked on error: " + throwable;
    error_skips = error_factor;
  }

  // called by timer thread
  void reportStopped() {
    status = "Thread stopped";
    if (logger != null)
      logger.error("Timer thread has stopped");
  }
  
  // --- Listener implementation
  
  public void batchReady(int size) {
    last_batch_size = size;
  }
  
  public void batchDone() {
    linkdb.commit();
    records += last_batch_size;
    lastRecord = System.currentTimeMillis();
  }

  // --- Create link database

  private LinkDatabase makeLinkDatabase(Properties props) {
    String dbtype = get(props, "duke.linkdbtype");
    if (dbtype.equals("jdbc"))
      return makeJDBCLinkDatabase(props);
    else if (dbtype.equals("jndi"))
      return makeJNDILinkDatabase(props);
    else
      throw new DukeConfigException("Unknown link database type '" + dbtype +
                                    "'");
  }

  private LinkDatabase makeJDBCLinkDatabase(Properties props) {
    String linkjdbcuri = get(props, "duke.linkjdbcuri");
    String driverklass = get(props, "duke.jdbcdriver");
    String dbtype = get(props, "duke.database");
    String tblprefix = get(props, "duke.table-prefix", null);

    Properties jdbcprops = new Properties();
    if (get(props, "duke.username", null) != null)
      jdbcprops.put("user", get(props, "duke.username"));
    if (get(props, "duke.password", null) != null)
      jdbcprops.put("password", get(props, "duke.password"));

    JDBCLinkDatabase db;
    db = new JDBCLinkDatabase(driverklass, linkjdbcuri, dbtype, jdbcprops);
    if (tblprefix != null)
      db.setTablePrefix(tblprefix);
    db.init();
    return db;
  }

  private LinkDatabase makeJNDILinkDatabase(Properties props) {
    String tblprefix = get(props, "duke.table-prefix", null);
    JNDILinkDatabase db = new JNDILinkDatabase(get(props, "duke.linkjndipath"),
                                               get(props, "duke.database"));
    if (tblprefix != null)
      db.setTablePrefix(tblprefix);
    db.init();
    return db;
  }
  
}