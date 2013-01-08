
package no.priv.garshol.duke;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collection;
import java.io.IOException;
import java.io.Console;
import java.io.Writer;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.lucene.index.CorruptIndexException;

import no.priv.garshol.duke.matchers.AbstractMatchListener;
import no.priv.garshol.duke.matchers.TestFileListener;
import no.priv.garshol.duke.matchers.PrintMatchListener;
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

    // parse command-line
    CommandLineParser parser = setupParser();
    try {
      argv = parser.parse(argv);
    } catch (CommandLineParser.CommandLineParserException e) {
      System.err.println("ERROR: " + e.getMessage());
      usage();
      System.exit(1);
    }

    // set up some initial options
    Logger logger = new CommandLineLogger(parser.getOptionState("verbose") ?
                                          1 : 0);
    boolean progress = parser.getOptionState("progress");
    int count = 0;
    int batch_size = 40000;
    if (parser.getOptionValue("batchsize") != null)
      batch_size = Integer.parseInt(parser.getOptionValue("batchsize"));

    // load the configuration
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

    // if we're in data debug mode we branch out here
    boolean datadebug = parser.getOptionState("showdata");
    if (datadebug) {
      showdata(config);
      return; // stop here
    }

    // set up listeners
    boolean noreindex = parser.getOptionState("noreindex");
    Processor processor;
    if (parser.getOptionValue("threads") == null)
      processor = new Processor(config, !noreindex);
    else {
      processor = new MultithreadProcessor2(config);
      ((MultithreadProcessor2) processor).setThreadCount(Integer.parseInt(parser.getOptionValue("threads")));
    }
    processor.setLogger(logger);

    // sanity check
    if (noreindex && processor.getDatabase().isInMemory()) {
      System.out.println("Option --noreindex not available with in-memory " +
                         "database");
      return;
    }
    
    boolean interactive = parser.getOptionState("interactive");
    boolean showmatches = parser.getOptionState("showmatches") || interactive;
    PrintMatchListener listener =
      new PrintMatchListener(showmatches,
                             parser.getOptionState("showmaybe"),
                             progress,
                             !config.isDeduplicationMode(),
                             config.getProperties(),
                             interactive);
    processor.addMatchListener(listener);

    // needs to be before the link file handler, in case the link file
    // is the same as the test file
    TestFileListener testfile = null;
    if (parser.getOptionValue("testfile") != null) {
      testfile = new TestFileListener(parser.getOptionValue("testfile"),
                                      config.getIdentityProperties(),
                                      parser.getOptionState("testdebug"),
                                      processor,
                                      !config.isDeduplicationMode(),
                                      showmatches);
      processor.addMatchListener(testfile);
    }
    
    AbstractLinkFileListener linkfile = null;
    if (parser.getOptionValue("linkfile") != null) {
      String fname = parser.getOptionValue("linkfile");
      if (fname.endsWith(".ntriples"))
        linkfile = new NTriplesLinkFileListener(fname, config.getIdentityProperties());
      else
        linkfile = new LinkFileListener(fname, config.getIdentityProperties(),
                                        interactive,
                                        parser.getOptionValue("testfile"));
      processor.addMatchListener(linkfile);
    }

    // this is where we get started for real. the first thing we do
    // is to distinguish between modes.
    if (config.isDeduplicationMode())
      // deduplication mode
      processor.deduplicate(config.getDataSources(), batch_size);
    else {
      // record linkage mode
      if (noreindex) {
        // user has specified that they already have group 1 indexed up,
        // and don't want to do it again, for whatever reason. in that
        // case we just do the linking, and don't touch group 1 at all.
        processor.linkRecords(config.getDataSources(2), false);
      } else
        processor.link(config.getDataSources(1),
                       config.getDataSources(2),
                       batch_size);
    }

    // close up shop, then finish
    if (parser.getOptionValue("linkfile") != null)
      linkfile.close();
    if (parser.getOptionValue("testfile") != null)
      testfile.close();
    processor.close();
  }

  private static void showdata(Configuration config) {
    List<Property> props = config.getProperties();
    System.out.println(props);
    
    for (DataSource src : config.getDataSources()) {
      RecordIterator it = src.getRecords();
      while (it.hasNext()) {
        Record r = it.next();
        PrintMatchListener.prettyPrint(r, props);
        System.out.println("");
      }
      it.close();
    }
  }
  
  private static void usage() throws IOException {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.Duke [options] <cfgfile>");
    System.out.println("");
    System.out.println("  --progress            show progress report while running");
    System.out.println("  --showmatches         show matches while running");
    System.out.println("  --linkfile=<file>     output matches to link file");
    System.out.println("  --interactive         query user before outputting link file matches");
    System.out.println("  --testfile=<file>     output accuracy stats");
    System.out.println("  --testdebug           display failures");
    System.out.println("  --verbose             display diagnostics");
    System.out.println("  --noreindex           reuse existing Lucene index");
    System.out.println("  --batchsize=n         set size of Lucene indexing batches");
    System.out.println("  --showdata            show all cleaned data (data debug mode)");
    System.out.println("");
    System.out.println("Duke version " + getVersionString());
  }

  private static CommandLineParser setupParser() {
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
    parser.registerOption(new CommandLineParser.StringOption("threads", 'P'));
    parser.registerOption(new CommandLineParser.BooleanOption("noreindex", 'N'));
    parser.registerOption(new CommandLineParser.BooleanOption("interactive", 'I'));
    parser.registerOption(new CommandLineParser.BooleanOption("showdata", 'D'));
    return parser;
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
    private Console console;
    private LinkDatabase linkdb;
    
    public LinkFileListener(String linkfile, Collection<Property> idprops,
                            boolean interactive, String testfile)
      throws IOException {
      super(idprops);
      if (interactive) {
        this.console = System.console();
        this.linkdb = new InMemoryLinkDatabase();

        if (testfile != null)
          loadTestFile(testfile);
      }

      // have to start writing the link file *after* we load the test
      // file, because they may be the same file...
      // second param: if there is a test file, we append to the link
      // file, instead of overwriting
      this.out = new FileWriter(linkfile, testfile != null);
      // FIXME: this will only work if the two files are the same
    }
    
    public void link(String id1, String id2) throws IOException {
      boolean correct = true;

      // does this provide new information, or do we know it already?
      Link inferredlink = null;
      if (linkdb != null)
        inferredlink = linkdb.inferLink(id1, id2);

      // record it
      if (console != null) {
        if (inferredlink == null)
          correct = console.readLine("Correct? (Y/N) ").equalsIgnoreCase("Y");
        else
          correct = inferredlink.getKind() == LinkKind.SAME;
      }
      // we only write the link out if it's not inferred. if it is
      // inferred we already have it in some other way (from original
      // test file, or from inference from links already written).
      if (inferredlink == null) {
        out.write((correct ? '+' : '-') + id1 + "," + id2 + "\n");
        out.flush(); // make sure we preserve the data
      }

      if (linkdb != null && inferredlink == null) {
        Link link = new Link(id1, id2, LinkStatus.ASSERTED,
                             correct ? LinkKind.SAME : LinkKind.DIFFERENT);
        linkdb.assertLink(link);
      }
    }
    
    public void close() throws IOException {
      out.close();
    }

    private void loadTestFile(String testfile) throws IOException {
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

        linkdb.assertLink(new Link(id1, id2, LinkStatus.ASSERTED,
                                   (line.charAt(0) == '+') ?
                                     LinkKind.SAME : LinkKind.DIFFERENT));
        
        // now read next line, and carry on
        line = reader.readLine();
      }

      reader.close();
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

    public void warn(String msg) {
      warn(msg, null);
    }

    public void warn(String msg, Throwable e) {
      if (!isWarnEnabled())
        return;

      System.out.println(msg + " " + e);
      e.printStackTrace();
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
      return loglevel == 1;
    }

    public boolean isDebugEnabled() {
      return loglevel != 0 && loglevel < 3;
    }

    public boolean isInfoEnabled() {
      return loglevel != 0 && loglevel < 4;
    }

    public boolean isWarnEnabled() {
      return loglevel != 0 && loglevel < 5;
    }

    public boolean isErrorEnabled() {
      return loglevel != 0 && loglevel < 6;
    }
  }
}