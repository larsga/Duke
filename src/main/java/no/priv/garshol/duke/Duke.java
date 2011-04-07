
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
    Database database = config.getDatabase();
    database.setMatchListener(new PrintMatchListener(database.getProperties()));
    Deduplicator dedup = new Deduplicator(database);
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

    if (!batch.isEmpty())
      dedup.process(batch);

    database.close();
  }

  private static void usage() {
    System.out.println("  java no.priv.garshol.duke.Duke <cfgfile>");
  }

  public static class PrintMatchListener implements MatchListener {
    private Collection<Property> properties;
    
    public PrintMatchListener(Collection<Property> properties) {
      this.properties = properties;
    }
    
    public void matches(Record r1, Record r2, double confidence) {
      System.out.println("MATCH " + confidence);      
      System.out.println(toString(r1));
      System.out.println(toString(r2));
    }

    private String toString(Record r) {
      StringBuffer buf = new StringBuffer();
      for (Property p : properties) {
        Collection<String> vs = r.getValues(p.getName());
        if (vs == null)
          continue;

        buf.append(p.getName() + ": ");          
        for (String v : vs)
          buf.append("'" + v + "', ");
      }
      return buf.toString();
    }
  }
}