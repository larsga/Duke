
package no.priv.garshol.duke;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;

/**
 * Command-line interface to the engine.
 */
public class Duke {

  public static void main(String[] argv)
    throws IOException, CorruptIndexException {
    if (argv.length != 1) {
      usage();
      System.exit(1);
    }

    int batch_size = 40000;
    
    Configuration config = ConfigLoader.load(argv[0]);
    System.out.println(config.getDatabase().getProperties());
    Deduplicator dedup = new Deduplicator(config.getDatabase());
    Collection<Record> batch = new ArrayList();
    
    Iterator<DataSource> it = config.getDataSources().iterator();
    while (it.hasNext()) {
      DataSource source = it.next();

      Iterator<Record> it2 = source.getRecords();
      while (it2.hasNext()) {
        batch.add(it2.next());
        if (batch.size() == batch_size) {
          dedup.process(batch);
          batch = new ArrayList();
        }
      }
    }
  }

  private static void usage() {
    System.out.println("  java no.priv.garshol.duke.Duke <cfgfile>");
  }
  
}