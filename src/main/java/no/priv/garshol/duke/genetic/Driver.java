
package no.priv.garshol.duke.genetic;

import java.io.IOException;
import org.xml.sax.SAXException;

import no.priv.garshol.duke.Duke;
import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.utils.CommandLineParser;

// Output configuration as XML
// Keep answers from user
//   (so that if rerunning don't have to answer again)
// Multithreading

/**
 * Command-line interface to the genetic algorithm.
 */
public class Driver {

  public static void main(String[] argv) throws IOException, SAXException {
    // parse command-line
    CommandLineParser parser = new CommandLineParser();
    parser.setMinimumArguments(1);
    parser.setMaximumArguments(1);
    parser.addStringOption("testfile", 'T');
    parser.addBooleanOption("scientific", 's');

    try {
      argv = parser.parse(argv);
    } catch (CommandLineParser.CommandLineParserException e) {
      System.err.println("ERROR: " + e.getMessage());
      usage();
      System.exit(1);
    }

    String testfile = parser.getOptionValue("testfile");

    // get started
    Configuration config = ConfigLoader.load(argv[0]);
    GeneticAlgorithm genetic =
      new GeneticAlgorithm(config, testfile,
                           parser.getOptionState("scientific"));
    genetic.run();

    // finally: display winning config
  }

  private static void usage() {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.genetic.Driver [options] <cfgfile>");
    System.out.println("");
    System.out.println("  --testfile=<file>     use a test file for evaluation");
    System.out.println("");
    System.out.println("Duke version " + Duke.getVersionString());
  }
  
}