
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;

import org.xml.sax.SAXException;

import no.priv.garshol.duke.utils.CommandLineParser;
import no.priv.garshol.duke.databases.KeyFunction;
import no.priv.garshol.duke.databases.MapDBBlockingDatabase;
import no.priv.garshol.duke.databases.AbstractBlockingDatabase;

/**
 * Outputs statistics on the contents of the Database in order to make
 * it easier for users to judge performance.
 */
public class DatabaseStatistics {

  public static void main(String[] argv) throws IOException, SAXException {
    // parse command line
    CommandLineParser parser = new CommandLineParser();
    parser.setMinimumArguments(1);
    parser.setMaximumArguments(1);
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
    Database database = config.getDatabase(reindex); // overwrite iff reindex
    if (database.isInMemory())
      reindex = true; // no other way to do it in this case

    // reindex, if requested
    if (reindex)
      reindex(config, database);

    // right kind of database?
    if (!(database instanceof AbstractBlockingDatabase)) {
      System.out.println("ERR: Only blocking databases are supported.");
      System.exit(2);
    }

    AbstractBlockingDatabase db = (AbstractBlockingDatabase) database;
    for (KeyFunction func : db.getKeyFunctions()) {
      System.out.println("===== " + func + "\n");

      // count sizes
      Map<Integer, Counter> sizes = new HashMap();
      Map blocks = db.getBlocks(func);
      for (Object block : blocks.values()) {
        int size;
        if (block instanceof String[])
          size = ((String[]) block).length;
        else if (block instanceof MapDBBlockingDatabase.Block)
          size = ((MapDBBlockingDatabase.Block) block).getIds().length;
        else
          throw new DukeException("Unknown block type: " + block);

        Counter c = sizes.get(size);
        if (c == null) {
          c = new Counter(size);
          sizes.put(size, c);
        }
        c.count += 1;
      }

      // output statistics
      List<Counter> counters = new ArrayList(sizes.values());
      Collections.sort(counters);
      for (Counter c : counters)
        System.out.println("" + c.size + ": " + c.count);
    }
  }

  private static void usage() {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.DatabaseStatistics <cfgfile>");
    System.out.println("");
    System.out.println("  --reindex: Reindex all records before comparing");
  }

  private static final int DEFAULT_BATCH_SIZE = 40000;
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

  static class Counter implements Comparable<Counter> {
    int size;
    int count;

    public Counter(int size) {
      this.size = size;
    }

    public int compareTo(Counter c) {
      return size - c.size;
    }
  }
}
