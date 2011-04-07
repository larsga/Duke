
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

    if (argv.length < 1 || argv.length > 2) {
      usage();
      System.exit(1);
    }

    boolean progress = argv[0].equals("--progress");
    int ix = progress ? 1 : 0;

    int count = 0;
    int batch_size = 40000;
    
    Configuration config = ConfigLoader.load(argv[ix]);
    Database database = config.getDatabase();
    database.setMatchListener(new PrintMatchListener(database.getProperties()));
    Deduplicator dedup = new Deduplicator(database);
    Collection<Record> batch = new ArrayList();
    
    Iterator<DataSource> it = config.getDataSources().iterator();
    while (it.hasNext()) {
      DataSource source = it.next();

      Iterator<Record> it2 = source.getRecords();
      while (it2.hasNext()) {
        Record record = it2.next();
        batch.add(record);
        count++;
        if (count % batch_size == 0) {
          System.out.println("Records: " + count);
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
    private int count;
    private Collection<Property> properties;
    
    public PrintMatchListener(Collection<Property> properties) {
      this.properties = properties;
      this.count = 0;
    }
    
    public void matches(Record r1, Record r2, double confidence) {
      count++;
      System.out.println("MATCH " + confidence);      
      System.out.println(toString(r1));
      System.out.println(toString(r2));
      if (count % 1000 == 0)
        System.out.println("" + count + "  matches");
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