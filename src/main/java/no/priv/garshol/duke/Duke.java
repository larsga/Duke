
package no.priv.garshol.duke;

import java.util.Map;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collection;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.lucene.index.CorruptIndexException;

import no.priv.garshol.duke.datasources.DataSource;
import no.priv.garshol.duke.utils.NTriplesWriter;
import no.priv.garshol.duke.utils.CommandLineParser;

/**
 * Command-line interface to the engine.
 */
public class Duke {
  private static Properties properties;

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
    parser.registerOption(new CommandLineParser.StringOption("linkendpoint", 'e'));
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
    
    Processor processor = new Processor(config);
    processor.setLogger(logger);
    PrintMatchListener listener =
      new PrintMatchListener(parser.getOptionState("showmatches"),
                             parser.getOptionState("showmaybe"),
                             progress);
    processor.addMatchListener(listener);

    AbstractLinkFileListener linkfile = null;
    if (parser.getOptionValue("linkfile") != null) {
      String fname = parser.getOptionValue("linkfile");
      if (fname.endsWith(".ntriples"))
        linkfile = new NTriplesLinkFileListener(fname, config.getIdentityProperties());
      else
        linkfile = new LinkFileListener(fname, config.getIdentityProperties());
      processor.addMatchListener(linkfile);
    }
    
    TestFileListener testfile = null;
    if (parser.getOptionValue("testfile") != null) {
      testfile = new TestFileListener(parser.getOptionValue("testfile"),
                                      config.getIdentityProperties(),
                                      parser.getOptionState("testdebug"),
                                      processor);
      processor.addMatchListener(testfile);
    }

    // this is where the two modes separate.
    if (!config.getDataSources().isEmpty())
      // deduplication mode
      processor.deduplicate(config.getDataSources(), batch_size);
    else
      // record linkage mode
      processor.link(config.getDataSources(1),
                     config.getDataSources(2),
                     batch_size);

    if (parser.getOptionValue("linkfile") != null)
      linkfile.close();
    if (parser.getOptionValue("testfile") != null)
      testfile.close();
    processor.close();
  }
  
  private static void usage() throws IOException {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.Duke [options] <cfgfile>");
    System.out.println("");
    System.out.println("  --progress            show progress report while running");
    System.out.println("  --showmatches         show matches while running");
    System.out.println("  --linkfile=<file>     output matches to link file");
    System.out.println("  --testfile=<file>     output accuracy stats");
    System.out.println("  --testdebug           display failures");
    System.out.println("  --verbose             display diagnostics");
    System.out.println("");
    System.out.println("Duke version " + getVersionString());
  }

  public static String getVersionString() throws IOException {
    Properties props = getProperties();
    return props.getProperty("duke.version") + ", build " +
           props.getProperty("duke.build") + ", built by " +
           props.getProperty("duke.builder");
  }
  
  private static Properties getProperties() throws IOException {
    if (properties == null) {
      properties = new Properties();
      InputStream in = Duke.class.getClassLoader().getResourceAsStream("no/priv/garshol/duke/duke.properties");
      properties.load(in);
      in.close();
    }
    return properties;
  }
  
  static abstract class AbstractLinkFileListener extends AbstractMatchListener {
    private Collection<Property> idprops;
    
    public AbstractLinkFileListener(Collection<Property> idprops) {
      this.idprops = idprops;
    }

    public void close() throws IOException {
    }

    public abstract void link(String id1, String id2) throws IOException;
    
    public void matches(Record r1, Record r2, double confidence) {
      try {
        for (Property p : idprops)
          for (String id1 : r1.getValues(p.getName()))
            for (String id2 : r2.getValues(p.getName()))
              link(id1, id2);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  static class LinkFileListener extends AbstractLinkFileListener {
    private Writer out;
    
    public LinkFileListener(String linkfile, Collection<Property> idprops)
      throws IOException {
      super(idprops);
      this.out = new FileWriter(linkfile);
    }
    
    public void link(String id1, String id2) throws IOException {
      out.write(id1 + "," + id2 + "\n");
    }
    
    public void close() throws IOException {
      out.close();
    }
  }

  static class NTriplesLinkFileListener extends AbstractLinkFileListener {
    private FileOutputStream fos;
    private NTriplesWriter out;
    
    public NTriplesLinkFileListener(String linkfile,
                                    Collection<Property> idprops)
      throws IOException {
      super(idprops);
      this.fos = new FileOutputStream(linkfile);
      this.out = new NTriplesWriter(fos);
    }
    
    public void link(String id1, String id2) throws IOException {
      out.statement(id1, "http://www.w3.org/2002/07/owl#sameAs", id2, false);
    }
    
    public void close() throws IOException {
      out.done();
      fos.close();
    }
  }
  
  static class TestFileListener extends AbstractMatchListener {
    private Collection<Property> idprops;
    private Map<String, Link> links;
    private int notintest;
    private int missed; // RL mode only
    private boolean debug;
    private Processor processor;
    private Database database;
    
    public TestFileListener(String testfile, Collection<Property> idprops,
                            boolean debug, Processor processor)
      throws IOException {
      this.idprops = idprops;
      this.links = load(testfile);
      this.debug = debug;
      this.processor = processor;
      this.database = processor.getDatabase();
    }

    public void close() throws IOException {
      int correct = 0;
      int correctfound = 0;
      int wrong = 0;
      int wrongfound = 0;
      
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
              PrintMatchListener.show(r1, r2, processor.compare(r1, r2),
                                      "\nNOT FOUND");
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
    private int loglevel; // 1: trace, 2: debug, 3: info, 4: warn, 5: error

    private CommandLineLogger(int loglevel) {
      this.loglevel = loglevel;
    }

    public void trace(String msg) {
      if (isTraceEnabled())
        System.out.println(msg);
    }
    
    public void debug(String msg) {
      if (isDebugEnabled())
        System.out.println(msg);
    }

    public void info(String msg) {
      if (isInfoEnabled())
        System.out.println(msg);
    }

    public void error(String msg) {
      error(msg, null);
    }

    public void error(String msg, Throwable e) {
      if (!isErrorEnabled())
        return;

      System.out.println(msg + " " + e);
      e.printStackTrace();
    }
    
    public boolean isTraceEnabled() {
      return loglevel > 0;
    }

    public boolean isDebugEnabled() {
      return loglevel > 1;
    }

    public boolean isInfoEnabled() {
      return loglevel > 2;
    }

    public boolean isErrorEnabled() {
      return loglevel > 4;
    }
  }
}