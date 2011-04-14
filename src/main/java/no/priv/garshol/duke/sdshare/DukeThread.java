
package no.priv.garshol.duke.sdshare;

import java.util.ArrayList;
import java.util.Collection;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.DataSource;
import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Deduplicator;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.RecordIterator;

/**
 * A thread which can be run inside an application server to set up
 * Duke to poll against a datasource for incoming records to process.
 */
public class DukeThread {
  private boolean stopped;
  
  public void run() throws Exception {
    Configuration config = ConfigLoader.load(""); // FIXME: what to load?
    Database database = config.getDatabase();
    // need a match listener...
    Deduplicator dedup = new Deduplicator(database);
    int batch_size = 40000;
    
    while (!stopped) {
      int count = 0;
      Collection<Record> batch = new ArrayList();

      for (DataSource source : config.getDataSources()) {
        RecordIterator it = source.getRecords();
        while (it.hasNext()) {
          Record record = it.next();
          batch.add(record);
          count++;
          if (count % batch_size == 0) {
            dedup.process(batch);
            batch = new ArrayList();
          }
        }
        it.close();
      }

      if (!batch.isEmpty())
        dedup.process(batch);

      // FIXME: sleep for a fixed time
    }
  }
}