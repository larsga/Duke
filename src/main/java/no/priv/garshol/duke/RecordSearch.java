
package no.priv.garshol.duke;

import java.util.Collection;
import java.io.IOException;

import org.xml.sax.SAXException;

import no.priv.garshol.duke.utils.CommandLineParser;
import no.priv.garshol.duke.matchers.PrintMatchListener;

/**
 * Search for records and display the matching ones.
 */
public class RecordSearch {
  private static final int MAX_SEARCH_HITS = 10;
  private static final int DEFAULT_BATCH_SIZE = 40000;

  public static void main(String[] argv) throws IOException, SAXException {
    // parse command line
    CommandLineParser parser = new CommandLineParser();
    parser.setMinimumArguments(3);
    parser.setMaximumArguments(3);
    parser.registerOption(new CommandLineParser.BooleanOption("reindex", 'I'));
    try {
      argv = parser.parse(argv);
    } catch (CommandLineParser.CommandLineParserException e) {
      System.err.println("ERROR: " + e.getMessage());
      usage();
      System.exit(1);
    }

    // do we need to reindex?
    boolean reindex = parser.getOptionState("reindex");
    
    // load configuration
    Configuration config = ConfigLoader.load(argv[0]);
    Database database = config.createDatabase(reindex); // overwrite iff reindex
    if (database.isInMemory())
      reindex = true; // no other way to do it in this case

    // reindex, if requested
    if (reindex)
      reindex(config, database);
  
    // is this lucene?
    if (!(database instanceof LuceneDatabase)) {
      System.out.println("Searching not supported for this backend: " + database);
      System.exit(1);
    }

    // search
    Collection<Record> records = ((LuceneDatabase) database).search(argv[1], argv[2]);
    for (Record record : records) {
      PrintMatchListener.prettyPrint(record, config.getProperties());
      System.out.println();
    }
  }

  private static void usage() {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.RecordSearch <cfgfile> <property> <query>");
    System.out.println("");
    System.out.println("  --reindex: Reindex all records before comparing");
  }

  private static void reindex(Configuration config, Database database) {
    System.out.println("Reindexing all records...");
    Processor processor = new Processor(config, database);
    if (config.isDeduplicationMode())
      processor.index(config.getDataSources(), DEFAULT_BATCH_SIZE);
    else {
      processor.index(config.getDataSources(1), DEFAULT_BATCH_SIZE);
      processor.index(config.getDataSources(2), DEFAULT_BATCH_SIZE);
    }
  }
  
}
