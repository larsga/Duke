
package no.priv.garshol.duke.genetic;

import java.io.IOException;
import org.xml.sax.SAXException;

import no.priv.garshol.duke.Duke;
import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.utils.CommandLineParser;

/**
 * Command-line interface to the genetic algorithm.
 */
public class Driver {

  public static void main(String[] argv) throws IOException, SAXException {
    // parse command-line
    CommandLineParser parser = new CommandLineParser();
    parser.setMinimumArguments(0);
    parser.setMaximumArguments(1);
    parser.addStringOption("testfile", 'T');
    parser.addBooleanOption("scientific", 's');
    parser.addStringOption("generations", 'G');
    parser.addStringOption("population", 'P');
    parser.addStringOption("questions", 'Q');
    parser.addStringOption("output", 'O');
	parser.addStringOption("dump-state", 'D');
	parser.addStringOption("restore-state", 'R');
    parser.addStringOption("threads", 't');
    parser.addBooleanOption("active", 'A');
    parser.addStringOption("linkfile", 'l');
    parser.addBooleanOption("sparse", 'S');
    parser.addStringOption("mutation-rate", 'm');
    parser.addStringOption("recombination-rate", 'r');
    parser.addBooleanOption("no-comparators", 'C');
    parser.addStringOption("original", 'o');
    parser.addBooleanOption("incomplete-data", 'I');

    try {
      argv = parser.parse(argv);
    } catch (CommandLineParser.CommandLineParserException e) {
      System.err.println("ERROR: " + e.getMessage());
      usage();
      System.exit(1);
    }

    String testfile = parser.getOptionValue("testfile");
    if (parser.getOptionState("scientific") && testfile == null) {
      System.err.println("ERROR: scientific mode requires a test file");
      System.exit(1);
    }
	 
	 String restoreStateDir = parser.getOptionValue("restore-state");
    if (argv.length == 0 && restoreStateDir == null) {
      System.err.println("ERROR: must specify a config file or be in restore-state mode");
      System.exit(1);
    }
	 
    if (argv.length == 1 && restoreStateDir != null) {
      System.out.println("WARNING: cannot specify a config file and be in restore-state mode. Ignoring restore-state option");
    }

    // get started
    Configuration config;
	 if (parser.getOptionValue("restore-state") != null) 
		 config = ConfigLoader.load(restoreStateDir + "//config_1.xml");
	 else
		 config = ConfigLoader.load(argv[0]);
		 
    GeneticAlgorithm genetic =
      new GeneticAlgorithm(config, testfile,
                           parser.getOptionState("scientific"));
    genetic.setPopulation(parser.getOptionInteger("population", 100));
    genetic.setGenerations(parser.getOptionInteger("generations", 100));
    genetic.setQuestions(parser.getOptionInteger("questions", 10));
    genetic.setConfigOutput(parser.getOptionValue("output"));
	genetic.setDumpStateDir(parser.getOptionValue("dump-state"));
	genetic.setRestoreStateDir(parser.getOptionValue("restore-state"));
    genetic.setThreads(parser.getOptionInteger("threads", 1));
    genetic.setSparse(parser.getOptionState("sparse"));
    genetic.setMutationRate(parser.getOptionInteger("mutation-rate", -1));
    genetic.setRecombinationRate(parser.getOptionDouble("recombination-rate", -1.0));
    genetic.setEvolveComparators(!parser.getOptionState("no-comparators"));
    genetic.setIncompleteTest(parser.getOptionState("incomplete-data"));
	 //Meaningless when restoring full population from saved state
	 genetic.setCopiesOfOriginal((restoreStateDir == null) ? parser.getOptionInteger("original", 0) : 0);
    if (parser.getOptionState("active"))
      genetic.setActive(true);
    if (parser.getOptionValue("linkfile") != null)
      genetic.setLinkFile(parser.getOptionValue("linkfile"));
    genetic.run();
  }

  private static void usage() {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.genetic.Driver [options] <cfgfile>");
    System.out.println("");
    System.out.println("  --testfile=<file>      use a test file for evaluation");
    System.out.println("  --active               use active learning, even if there is a test file");
    System.out.println("  --generations=N        number of generations to run (100)");
    System.out.println("  --population=N         number of configurations in population (100)");
    System.out.println("  --questions=N          questions to ask per generation (10)");
    System.out.println("  --sparse               don't ask questions after every generation");
    System.out.println("  --output=<file>        file to write best configuration to");
    System.out.println("                         (a new export after every generation)");
	 System.out.println("  --dump-state=<dir>    directory to dump the current state to");
    System.out.println("                         (dumps after every generation)");
	 System.out.println("  --restore-state=<dir> directory to load state from (restart from saved state)");
    System.out.println("  --threads=N            number of threads to run");
    System.out.println("  --linkfile=<file>      write user's answers to this file");
    System.out.println("  --scientific           test active learning");
    System.out.println("  --mutation-rate=n      mutation rate (default: self-evolving)");
    System.out.println("  --recombination-rate=n recombination rate (default: self-evolving)");
    System.out.println("  --no-comparators       don't evolve comparators");
    System.out.println("  --original=N           keep N copies of the original configuration");
    System.out.println("  --incomplete-data      use test file for training, but assume it's incomplete");
    System.out.println("");
    System.out.println("Duke version " + Duke.getVersionString());
  }

}
