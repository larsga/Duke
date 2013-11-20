
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

import org.xml.sax.SAXException;

import no.priv.garshol.duke.utils.Utils;
import no.priv.garshol.duke.utils.CommandLineParser;

/**
 * Compare two specific records to understand their scores.
 */
public class DebugCompare {
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
  
    // load records
    Record r1 = database.findRecordById(argv[1]);
    if (r1 == null) {
      System.err.println("Couldn't find record for '" + argv[1] + "'");
      System.err.println("Consider using --reindex");
      return;
    }    
    Record r2 = database.findRecordById(argv[2]);
    if (r2 == null) {
      System.err.println("Couldn't find record for '" + argv[2] + "'");
      System.err.println("Consider using --reindex");
      return;
    }

    // do comparison
    double prob = 0.5;
    for (Property prop : config.getProperties()) {
      if (prop.isIdProperty())
        continue;

      String propname = prop.getName();
      System.out.println("---" + propname);

      Collection<String> vs1 = r1.getValues(propname);
      Collection<String> vs2 = r2.getValues(propname);
      if (vs1.isEmpty() || vs2.isEmpty() || prop.isIgnoreProperty())
        continue; // no values to compare, so skip (or property is type=ignore)
      
      double high = 0.0;
      for (String v1 : vs1) {
        if (v1.equals(""))
          continue;
        
        for (String v2 : vs2) {
          if (v2.equals(""))
            continue;

          try {
            Comparator comp = prop.getComparator();
            if (comp == null) {
              high = 0.5; // no comparator, so we learn nothing
              break;
            }
            
            double d = comp.compare(v1, v2);
            double p = prop.compare(v1, v2);
            System.out.println("'" + v1 + "' ~ '" + v2 + "': " + d +
                               " (prob " + p + ")");
            high = Math.max(high, p);
          } catch (Exception e) {
            throw new DukeException("Comparison of values '" + v1 + "' and "+
                                    "'" + v2 + "' failed", e);
          }
        }        
      }

      double newprob = Utils.computeBayes(prob, high);
      System.out.println("Result: " + prob + " -> " + newprob + "\n");
      prob = newprob;
    }

    System.out.println("Overall: " + prob);
  }

  private static void usage() {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.DebugCompare <cfgfile> <id1> <id2>");
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
