
package no.priv.garshol.duke;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import no.priv.garshol.duke.matchers.AbstractMatchListener;
import no.priv.garshol.duke.matchers.PrintMatchListener;
import no.priv.garshol.duke.matchers.TestFileListener;
import no.priv.garshol.duke.utils.YesNoConsole;
import no.priv.garshol.duke.utils.LinkFileWriter;
import no.priv.garshol.duke.utils.NTriplesWriter;
import no.priv.garshol.duke.utils.LinkDatabaseUtils;
import no.priv.garshol.duke.utils.CommandLineParser;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Command-line interface to the engine.
 */
public class Duke {
  private static Properties properties;

  public static void main(String[] argv) throws IOException {
    try {
      main_(argv);
    } catch (DukeConfigException e) {
      System.err.println("ERROR: " + e.getMessage());
    }
  }

  public static void main_(String[] argv) throws IOException {
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
    boolean datadebug = parser.getOptionState("showdata");
    Logger logger = new CommandLineLogger(parser.getOptionState("verbose") ?
                                          1 : 0);
    boolean progress = parser.getOptionState("progress");
    int count = 0;
    int batch_size = parser.getOptionInteger("batchsize", 40000);
    int threads = parser.getOptionInteger("threads", 1);

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

    // validate the configuration
    if (!datadebug) // unless --showdata
      config.validate();

    // if we're in data debug mode we branch out here
    if (datadebug) {
      showdata(config);
      return; // stop here
    }

    // set up listeners
    boolean noreindex = parser.getOptionState("noreindex");
    Processor processor = new Processor(config, !noreindex);
    processor.setLogger(logger);
    processor.setThreads(threads);

    // sanity check
    if (noreindex && processor.getDatabase().isInMemory()) {
      System.out.println("Option --noreindex not available with in-memory " +
                         "database");
      return;
    }

    // display lookup properties?
    if (parser.getOptionState("lookups")) {
      System.out.println("Lookup properties:");
      for (Property p : config.getLookupProperties())
        System.out.println("  " + p.getName());
      System.out.println();
    }

    boolean interactive = parser.getOptionState("interactive");
    boolean pretty = parser.getOptionState("pretty") || interactive;
    boolean showmatches = parser.getOptionState("showmatches") || interactive;
    PrintMatchListener listener =
      new PrintMatchListener(showmatches,
                             parser.getOptionState("showmaybe"),
                             progress,
                             !config.isDeduplicationMode(),
                             config.getProperties(),
                             pretty);
    processor.addMatchListener(listener);

    // needs to be before the link file handler, in case the link file
    // is the same as the test file
    TestFileListener testfile = null;
    if (parser.getOptionValue("testfile") != null) {
      testfile = new TestFileListener(parser.getOptionValue("testfile"),
                                      config,
                                      parser.getOptionState("testdebug"),
                                      processor,
                                      showmatches,
                                      pretty);
      testfile.setPessimistic(true);
      processor.addMatchListener(testfile);

      if (testfile.isEmpty())
        System.out.println("WARN: Test file is empty. Did you mean --linkfile?");
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

    // --profile
    if (parser.getOptionState("profile"))
      processor.setPerformanceProfiling(true);

    // --singlematch setting
    boolean matchall = true;
    if (parser.getOptionState("singlematch")) {
      if (config.isDeduplicationMode())
        throw new DukeConfigException("--singlematch only works in record linkage mode");
      matchall = false;
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
        processor.linkRecords(config.getDataSources(2), matchall);
      } else
        processor.link(config.getDataSources(1),
                       config.getDataSources(2),
                       matchall,
                       batch_size);
    }

    // close up shop, then finish
    if (parser.getOptionValue("linkfile") != null)
      linkfile.close();
    processor.close();
  }

  private static void showdata(Configuration config) {
    List<Property> props = config.getProperties();
    List<DataSource> sources = new ArrayList();
    sources.addAll(config.getDataSources());
    sources.addAll(config.getDataSources(1));
    sources.addAll(config.getDataSources(2));

    for (DataSource src : sources) {
      RecordIterator it = src.getRecords();
      while (it.hasNext()) {
        Record r = it.next();
        PrintMatchListener.prettyPrint(r, props);
        System.out.println("");
      }
      it.close();
    }
  }

  private static void usage() {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.Duke [options] <cfgfile>");
    System.out.println("");
    System.out.println("  --progress            show progress report while running");
    System.out.println("  --showmatches         show matches while running");
    System.out.println("  --linkfile=<file>     output matches to link file");
    System.out.println("  --interactive         query user before outputting link file matches");
    System.out.println("  --testfile=<file>     test matches against known correct results in file");
    System.out.println("  --testdebug           display failures");
    System.out.println("  --verbose             display diagnostics");
    System.out.println("  --noreindex           reuse existing Lucene index");
    System.out.println("  --batchsize=n         set size of Lucene indexing batches");
    System.out.println("  --showdata            show all cleaned data (data debug mode)");
    System.out.println("  --profile             display performance statistics");
    System.out.println("  --threads=N           run processing in N parallell threads");
    System.out.println("  --pretty              pretty display when comparing records");
    System.out.println("  --singlematch         (in record linkage mode) only accept");
    System.out.println("                        the best match for each record");
    System.out.println("  --lookups             display lookup properties");
    System.out.println("");
    System.out.println("Duke version " + getVersionString());
  }

  private static CommandLineParser setupParser() {
    CommandLineParser parser = new CommandLineParser();
    parser.setMinimumArguments(1);
    parser.setMaximumArguments(1);
    parser.addBooleanOption("progress", 'p');
    parser.addStringOption("linkfile", 'l');
    parser.addStringOption("linkendpoint", 'e');
    parser.addBooleanOption("showmatches", 's');
    parser.addBooleanOption("showmaybe", 'm');
    parser.addStringOption("testfile", 'T');
    parser.addBooleanOption("testdebug", 't');
    parser.addStringOption("batchsize", 'b');
    parser.addBooleanOption("verbose", 'v');
    parser.addStringOption("threads", 'P');
    parser.addBooleanOption("noreindex", 'N');
    parser.addBooleanOption("interactive", 'I');
    parser.addBooleanOption("showdata", 'D');
    parser.addBooleanOption("profile", 'o');
    parser.addStringOption("threads", 'n');
    parser.addBooleanOption("pretty", 'n');
    parser.addBooleanOption("singlematch", 'n');
    parser.addBooleanOption("lookups", 'L');
    return parser;
  }

  public static String getVersionString() {
    Properties props = getProperties();
    return props.getProperty("duke.version") + ", build " +
           props.getProperty("duke.build") + ", built by " +
           props.getProperty("duke.builder");
  }

  public static String getVersion() {
    return getProperties().getProperty("duke.version");
  }

  private static Properties getProperties() {
    if (properties == null) {
      properties = new Properties();
      try {
        InputStream in = Duke.class.getClassLoader().getResourceAsStream("no/priv/garshol/duke/duke.properties");
        properties.load(in);
        in.close();
      } catch (IOException e) {
        throw new DukeException("Couldn't load duke.properties", e);
      }
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

    public abstract void link(String id1, String id2, double confidence)
      throws IOException;

    public void matches(Record r1, Record r2, double confidence) {
      try {
        for (Property p : idprops)
          for (String id1 : r1.getValues(p.getName()))
            for (String id2 : r2.getValues(p.getName()))
              link(id1, id2, confidence);
      } catch (IOException e) {
        throw new DukeException(e);
      }
    }
  }

  static class LinkFileListener extends AbstractLinkFileListener {
    private Writer out;
    private LinkFileWriter writer;
    private LinkDatabase linkdb;
    private YesNoConsole console;

    public LinkFileListener(String linkfile, Collection<Property> idprops,
                            boolean interactive, String testfile)
      throws IOException {
      super(idprops);
      if (interactive) {
        this.console = new YesNoConsole();
        this.linkdb = new InMemoryLinkDatabase();

        if (testfile != null)
          linkdb = LinkDatabaseUtils.loadTestFile(testfile);
      }

      // have to start writing the link file *after* we load the test
      // file, because they may be the same file...
      // second param: if there is a test file, we append to the link
      // file, instead of overwriting
      this.out = new FileWriter(linkfile, testfile != null);
      this.writer = new LinkFileWriter(out);
      // FIXME: this will only work if the two files are the same
    }

    public void link(String id1, String id2, double confidence)
      throws IOException {
      boolean correct = true;

      // does this provide new information, or do we know it already?
      Link inferredlink = null;
      if (linkdb != null)
        inferredlink = linkdb.inferLink(id1, id2);

      // record it
      if (console != null) {
        if (inferredlink == null) {
          correct = console.yesorno();
          confidence = 1.0; // the user told us, which is as certain as it gets
        } else {
          correct = inferredlink.getKind() == LinkKind.SAME;
          confidence = inferredlink.getConfidence();
        }
      }

      // note that we also write inferred links, because the test file
      // listener does not do inference
      writer.write(id1, id2, correct, confidence);
      out.flush(); // make sure we preserve the data

      if (linkdb != null && inferredlink == null) {
        Link link = new Link(id1, id2, LinkStatus.ASSERTED,
                             correct ? LinkKind.SAME : LinkKind.DIFFERENT, 1.0);
        linkdb.assertLink(link);
      }
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

    public void link(String id1, String id2, double confidence)
      throws IOException {
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
