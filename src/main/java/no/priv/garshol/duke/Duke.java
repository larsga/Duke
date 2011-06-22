
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
import java.io.FileNotFoundException;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.lucene.index.CorruptIndexException;

/**
 * Command-line interface to the engine.
 */
public class Duke {

  public static void main(String[] argv)
    throws IOException, CorruptIndexException {
    try {
      main_(argv);
    } catch (DukeConfigException e) {
      System.err.println("ERROR: " + e.getMessage());
    }
  }

  public static void main_(String[] argv)
    throws IOException, CorruptIndexException {
    CommandLineParser parser = new CommandLineParser();
    parser.setMinimumArguments(1);
    parser.registerOption(new CommandLineParser.BooleanOption("progress", 'p'));
    parser.registerOption(new CommandLineParser.StringOption("linkfile", 'l'));
    parser.registerOption(new CommandLineParser.BooleanOption("showmatches", 's'));
    parser.registerOption(new CommandLineParser.BooleanOption("showmaybe", 'm'));
    parser.registerOption(new CommandLineParser.StringOption("testfile", 'T'));
    parser.registerOption(new CommandLineParser.BooleanOption("testdebug", 't'));
    parser.registerOption(new CommandLineParser.StringOption("batchsize", 'b'));
    parser.registerOption(new CommandLineParser.BooleanOption("verbose", 'v'));

    try {
      argv = parser.parse(argv);
    } catch (CommandLineParser.CommandLineParserException e) {
      System.out.println("ERROR: " + e.getMessage());
      usage();
      System.exit(1);
    }

    Logger logger = new CommandLineLogger(parser.getOptionState("verbose") ?
                                          1 : 0);
    boolean progress = parser.getOptionState("progress");
    int count = 0;
    int batch_size = 40000;
    if (parser.getOptionValue("batchsize") != null)
      batch_size = Integer.parseInt(parser.getOptionValue("batchsize"));
    
    Configuration config;
    try {
      config = ConfigLoader.load(argv[0]);
    } catch (FileNotFoundException e) {
      System.err.println("ERROR: Config file '" + argv[0] + "' not found!");
      return;
    } catch (SAXParseException e) {
      System.err.println("ERROR: Couldn't parse config file: " + e.getMessage());
      System.err.println("Error in " + e.getSystemId() + ":" +
                         e.getLineNumber() + ":" + e.getColumnNumber());
      return;
    } catch (SAXException e) {
      System.err.println("ERROR: Couldn't parse config file: " + e.getMessage());
      return;
    }
    
    Database database = config.getDatabase(true);
    PrintMatchListener listener =
      new PrintMatchListener(parser.getOptionState("showmatches"),
                             parser.getOptionState("showmaybe"),
                             progress);
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

    // this is where the two modes separate.
    Deduplicator dedup = new Deduplicator(database);
    if (!config.getDataSources().isEmpty())
      // deduplication mode
      dedup.deduplicate(config.getDataSources(), logger, batch_size);
    else
      // record linkage mode
      dedup.link(config.getDataSources(1),
                 config.getDataSources(2),
                 logger, batch_size);

    if (parser.getOptionValue("linkfile") != null)
      linkfile.close();
    if (parser.getOptionValue("testfile") != null)
      testfile.close();
    database.close();
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
    System.out.println("  --verbose          display diagnostics");
    System.out.println("");
  }

  static class LinkFileListener extends AbstractMatchListener {
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

  static class TestFileListener extends AbstractMatchListener {
    private Collection<Property> idprops;
    private Map<String, Link> links;
    private int notintest;
    private int missed; // RL mode only
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
            Record r1 = database.findRecordById(link.id1);
            Record r2 = database.findRecordById(link.id2);
            if (r1 != null && r2 != null)
              PrintMatchListener.show(r1, r2, dedup.compare(r1, r2), "\nNOT FOUND");
            else {
              System.out.println("\nIDENTITIES IN TEST FILE NOT FOUND IN DATA");
              System.out.println("ID1: " + link.id1 + " -> " + r1);
              System.out.println("ID2: " + link.id2 + " -> " + r2);
            }
          }
        } else {
          wrong++;
          if (link.asserted)
            wrongfound++;
        }
      }

      int total = correctfound + wrongfound + notintest;
      System.out.println("");
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
      if (missed > 0)
        System.out.println("Records with no link: " + missed);

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
              if (!link.correct && debug)
                PrintMatchListener.show(r1, r2, confidence, "\nINCORRECT");
              break;
            }
          }

      if (!found) {
        notintest++;
        if (debug)
          PrintMatchListener.show(r1, r2, confidence, "\nNOT IN TEST FILE");
      }
    }
   
    // called in RL mode when we don't find any matches for a record.
    public void noMatchFor(Record record) {
      System.out.println("\nNO MATCHING RECORD");
      System.out.println(PrintMatchListener.toString(record));

      // GRRR! we can't work out here whether this miss is in the test file
      // or not
      missed++;
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

  static class CommandLineLogger implements Logger {
    private int loglevel;

    private CommandLineLogger(int loglevel) {
      this.loglevel = loglevel;
    }
    
    public void debug(String msg) {
      if (loglevel > 0)
        System.out.println(msg);
    }
  }
}