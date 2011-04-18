
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;

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
    parser.registerOption(new CommandLineParser.StringOption("testfile", 'T'));
    parser.registerOption(new CommandLineParser.BooleanOption("testdebug", 't'));

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

    TestFileListener testfile = null;
    if (parser.getOptionValue("testfile") != null) {
      testfile = new TestFileListener(parser.getOptionValue("testfile"),
                                      database.getIdentityProperties(),
                                      parser.getOptionState("testdebug"),
                                      database);
      database.addMatchListener(testfile);
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
    if (parser.getOptionValue("linkfile") != null)
      linkfile.close();
    if (parser.getOptionValue("testfile") != null)
      testfile.close();
    database.close();
  }

  private static void show(Record r1, Record r2, double confidence) {
    System.out.println("MATCH " + confidence);      
    System.out.println(toString(r1));
    System.out.println(toString(r2));
  }

  private static String toString(Record r) {
    StringBuffer buf = new StringBuffer();
    for (String p : r.getProperties()) {
      Collection<String> vs = r.getValues(p);
      if (vs == null)
        continue;
      
      buf.append(p + ": ");          
      for (String v : vs)
        buf.append("'" + v + "', ");
    }
    return buf.toString();
  }
  
  private static void usage() {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.Duke [options] <cfgfile>");
    System.out.println("");
    System.out.println("  --progress         show progress report while running");
    System.out.println("  --showmatches      show matches while running");
    System.out.println("  --linkfile=<file>  output matches to link file");
    System.out.println("  --testfile=<file>  output accuracy stats");
    System.out.println("  --testdebug        display failures");
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
      if (showmatches)
        show(r1, r2, confidence);
      if (count % 1000 == 0 && progress)
        System.out.println("" + count + "  matches");
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

  static class TestFileListener implements MatchListener {
    private Collection<Property> idprops;
    private Map<String, Link> links;
    private int notintest;
    private boolean debug;
    private Database database;
    
    public TestFileListener(String testfile, Collection<Property> idprops,
                            boolean debug, Database database)
      throws IOException {
      this.idprops = idprops;
      this.links = load(testfile);
      this.debug = debug;
      this.database = database;
    }

    public void close() throws IOException {
      int correct = 0;
      int correctfound = 0;
      int wrong = 0;
      int wrongfound = 0;
      
      // FIXME: should we really need this object? merge with Database?
      Deduplicator dedup = new Deduplicator(database);
      
      for (Link link : links.values()) {
        if (link.correct) {
          correct++;
          if (link.asserted)
            correctfound++;
          else if (debug) {
            // we missed this one
            System.out.println("NOT FOUND");
            Record r1 = database.findRecordById(link.id1);
            Record r2 = database.findRecordById(link.id2);
            show(r1, r2, dedup.compare(r1, r2));
          }
        } else {
          wrong++;
          if (link.asserted)
            wrongfound++;
        }
      }

      int total = correctfound + wrongfound + notintest;
      System.out.println("Correct links found: " + correctfound + " / " +
                         correct + " (" + percent(correctfound, correct) + "%)");
      System.out.println("Wrong links found: " + wrongfound + " / " +
                         wrong + " (" + percent(wrongfound, wrong) + "%)");
      System.out.println("Unknown links found: " + notintest);
      System.out.println("Percent of links correct " +
                         percent(correctfound, total) +
                         "%, wrong " +
                         percent(wrongfound, total) +
                         "%, unknown " +
                         percent(notintest, total) + "%");

      double precision = ((double) correctfound) / total;
      double recall = ((double) correctfound) / correct;
      double f = 2 * ((precision * recall) / (precision + recall));
      System.out.println("Precision " + percent(correctfound, total) +
                         "%, recall " + percent(correctfound, correct) +
                         "%, f-number " + f);
    }
    
    public void matches(Record r1, Record r2, double confidence) {
      boolean found = false;
      for (Property p : idprops)
        for (String id1 : r1.getValues(p.getName()))
          for (String id2 : r2.getValues(p.getName())) {
            if (id1.compareTo(id2) < 0) {
              String tmp = id1;
              id1 = id2;
              id2 = tmp;
            }

            Link link = links.get(id1 + "," + id2);
            if (link != null) {
              found = true;
              link.asserted();
              if (!link.correct && debug) {
                System.out.println("INCORRECT");
                show(r1, r2, confidence);
              }
              break;
            }
          }

      if (!found) {
        notintest++;
        if (debug) {
          System.out.println("NOT IN TEST FILE");
          show(r1, r2, confidence);
        }
      }
    }

    private String percent(int part, int total) {
      int p = (int) (((double) part * 1000) / (double) total);
      return "" + (p / 10.0);
    }

    private Map<String, Link> load(String testfile) throws IOException {
      Map<String, Link> links = new HashMap();
      BufferedReader reader = new BufferedReader(new FileReader(testfile));
      String line = reader.readLine();
      while (line != null) {
        int pos = line.indexOf(',');

        String id1 = line.substring(1, pos);
        String id2 = line.substring(pos + 1, line.length());
        if (id1.compareTo(id2) < 0) {
          String tmp = id1;
          id1 = id2;
          id2 = tmp;
        }

        links.put(id1 + "," + id2, new Link(id1, id2, line.charAt(0) == '+'));
        
        line = reader.readLine();
      }

      return links;
    }
  }

  static class Link {
    private String id1; // this is always lexicographically lower than id2
    private String id2;
    private boolean correct;  // is this link correct?
    private boolean asserted; // did Duke assert this link?

    public Link(String id1, String id2, boolean correct) {
      this.id1 = id1;
      this.id2 = id2;
      this.correct = correct;
    }

    public void asserted() {
      asserted = true;
    }
  }
}