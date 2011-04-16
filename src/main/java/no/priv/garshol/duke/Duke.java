
package no.priv.garshol.duke;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;

import org.apache.lucene.index.CorruptIndexException;

/**
 * Command-line interface to the engine.
 */
public class Duke {

  public static void main(String[] argv)
    throws IOException, CorruptIndexException {

    CommandLineParser parser = new CommandLineParser();
    parser.setMinimumArguments(1);
    parser.registerOption(new CommandLineParser.BooleanOption("progress", 'p'));
    parser.registerOption(new CommandLineParser.StringOption("linkfile", 'l'));
    parser.registerOption(new CommandLineParser.BooleanOption("showmatches", 's'));

    try {
      argv = parser.parse(argv);
    } catch (CommandLineParser.CommandLineParserException e) {
      System.out.println("ERROR: " + e.getMessage());
      usage();
      System.exit(1);
    }

    boolean progress = parser.getOptionState("progress");
    int count = 0;
    int batch_size = 40000;
    
    Configuration config = ConfigLoader.load(argv[0]);
    Database database = config.getDatabase();
    PrintMatchListener listener =
      new PrintMatchListener(database.getProperties(),
                             parser.getOptionState("showmatches"), progress);
    database.addMatchListener(listener);
    LinkFileListener linkfile = null;
    if (parser.getOptionValue("linkfile") != null) {
      linkfile = new LinkFileListener(parser.getOptionValue("linkfile"),
                                      database.getIdentityProperties());
      database.addMatchListener(linkfile);
    }
    Deduplicator dedup = new Deduplicator(database);
    Collection<Record> batch = new ArrayList();
    
    Iterator<DataSource> it = config.getDataSources().iterator();
    while (it.hasNext()) {
      DataSource source = it.next();

      RecordIterator it2 = source.getRecords();
      while (it2.hasNext()) {
        Record record = it2.next();
        batch.add(record);
        count++;
        if (count % batch_size == 0) {
          if (progress)
            System.out.println("Records: " + count);
          dedup.process(batch);
          batch = new ArrayList();
        }
      }
      it2.close();
    }

    if (!batch.isEmpty())
      dedup.process(batch);

    if (progress) {
      System.out.println("Total records: " + count);
      System.out.println("Total matches: " + listener.getMatchCount());
    }
    database.close();
    if (parser.getOptionValue("linkfile") != null)
      linkfile.close();
  }

  private static void usage() {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.Duke [options] <cfgfile>");
    System.out.println("");
    System.out.println("  --progress         show progress report while running");
    System.out.println("  --showmatches      show matches while running");
    System.out.println("  --linkfile=<file>  output matches to link file");
    System.out.println("");
  }

  static class PrintMatchListener implements MatchListener {
    private int count;
    private Collection<Property> properties;
    private boolean showmatches;
    private boolean progress;
    
    public PrintMatchListener(Collection<Property> properties,
                              boolean showmatches, boolean progress) {
      this.properties = properties;
      this.count = 0;
      this.showmatches = showmatches;
      this.progress = progress;
    }

    public int getMatchCount() {
      return count;
    }
    
    public void matches(Record r1, Record r2, double confidence) {
      count++;
      if (showmatches) {
        System.out.println("MATCH " + confidence);      
        System.out.println(toString(r1));
        System.out.println(toString(r2));
      }
      if (count % 1000 == 0 && progress)
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

  static class LinkFileListener implements MatchListener {
    private Writer out;
    private Collection<Property> idprops;
    
    public LinkFileListener(String linkfile, Collection<Property> idprops)
      throws IOException {
      this.out = new FileWriter(linkfile);
      this.idprops = idprops;
    }

    public void close() throws IOException {
      out.close();
    }
    
    public void matches(Record r1, Record r2, double confidence) {
       try {
        for (Property p : idprops)
          for (String id1 : r1.getValues(p.getName()))
            for (String id2 : r2.getValues(p.getName()))
              out.write(id1 + "," + id2 + "\n");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}