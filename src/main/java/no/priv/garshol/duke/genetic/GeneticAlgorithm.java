
package no.priv.garshol.duke.genetic;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.io.IOException;

import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.LinkStatus;
import no.priv.garshol.duke.DataSource;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.ConfigWriter;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.InMemoryLinkDatabase;
import no.priv.garshol.duke.utils.LinkDatabaseUtils;
import no.priv.garshol.duke.matchers.MatchListener;
import no.priv.garshol.duke.matchers.TestFileListener;
import no.priv.garshol.duke.matchers.PrintMatchListener;

/**
 * The class that actually runs the genetic algorithm.
 */
public class GeneticAlgorithm {
  private Configuration config;
  private GeneticPopulation population;
  private Database database;
  private Map<String, Record> secondary; // used in record linkage mode
  private InMemoryLinkDatabase testdb;
  private double best; // best ever
  private boolean active; // true iff we are using active learning
  private boolean scientific;
  private Oracle oracle;
  private String outfile; // file to write config to

  private int generations;
  private int questions; // number of questions to ask per iteration

  /**
   * Creates the algorithm.
   * @param scientific A mode used for testing. Set to false.
   */
  public GeneticAlgorithm(Configuration config, String testfile,
                          boolean scientific)
    throws IOException {
    this.config = config;
    this.population = new GeneticPopulation(config);
    this.generations = 100;
    this.questions = 10;
    this.testdb = new InMemoryLinkDatabase();
    testdb.setDoInference(true);
    this.scientific = scientific;

    if (!scientific) {
      this.oracle = new ConsoleOracle();
      if (testfile != null)
        LinkDatabaseUtils.loadTestFile(testfile, testdb);
      else
        active = true;
    } else {
      // in scientific mode we simulate active learning by pretending
      // not to have a test file, but answering all questions from the
      // test file. this allows us to evaluate how well the active
      // learning approach actually works.
      active = true;
      this.oracle = new LinkFileOracle(testfile);
    }
  }

  public void setGenerations(int generations) {
    this.generations = generations;
  }

  public void setPopulation(int population) {
    this.population.setSize(population);
  }

  public void setQuestions(int questions) {
    this.questions = questions;
  }

  public void setConfigOutput(String output) {
    this.outfile = output;
  }
  
  /**
   * Actually runs the genetic algorithm.
   */
  public void run() {
    // first index up all records
    Collection<DataSource> sources;
    if (config.isDeduplicationMode())
      sources = config.getDataSources();
    else
      sources = config.getDataSources(1);
      
    database = config.createDatabase(true);
    for (DataSource src : sources) {
      RecordIterator it = src.getRecords();
      while (it.hasNext())
        database.index(it.next());
    }
    database.commit();

    // remember second set of records, too
    if (!config.isDeduplicationMode() && active) {
      // in record linkage mode we need to be able to look up records
      // in the second group, so that we can show them to the user
      // when asking questions about them
      secondary = new HashMap();
      for (DataSource src : config.getDataSources(2)) {
        RecordIterator it = src.getRecords();
        while (it.hasNext()) {
          Record r = it.next();
          secondary.put(getid(r), r);
        }
      }
    }
    
    // make first, random population
    population.create();

    // run through the required number of generations
    for (int gen = 0; gen < generations; gen++) {
      System.out.println("===== GENERATION " + gen);
      evolve(gen);
    }
  }

  /**
   * Creates a new generation.
   * @param gen_no The number of the generation. The first is 0.
   */
  public void evolve(int gen_no) {
    // evaluate current generation
    List<GeneticConfiguration> pop = population.getConfigs();
    ExemplarsTracker tracker = null;
    if (active) {
      // the first time we try to find correct matches so that we're
      // guranteed the algorithm knows about *some* correct matches
      Comparator comparator = gen_no == 0 ?
        new FindCorrectComparator() : new DisagreementComparator();
      tracker = new ExemplarsTracker(config, comparator);
    }
    for (GeneticConfiguration cfg : pop) {
      double f = evaluate(cfg, testdb, tracker);
      cfg.setFNumber(f);
      System.out.println("  " + f);
      if (f > best) {
        System.out.println("\nNEW BEST!\n");
        best = f;
      }
    }

    population.sort();

    // compute some key statistics
    double fsum = 0.0;
    double lbest = 0.0;
    for (GeneticConfiguration cfg : pop) {
      fsum += cfg.getFNumber();
      if (cfg.getFNumber() > lbest)
        lbest = cfg.getFNumber();
    }
    System.out.println("BEST: " + lbest + " AVERAGE: " + (fsum / pop.size()));
    for (GeneticConfiguration cfg : population.getConfigs())
      System.out.print(cfg.getFNumber() + " ");
    System.out.println();

    // in scientific mode, work out how good the best configuration actually is
    if (scientific) {
      GeneticConfiguration cfg = population.getBestConfiguration();
      double f = evaluate(cfg, ((LinkFileOracle) oracle).getLinkDatabase(),
                          null);
      System.out.println("\nACTUAL SCORE OF BEST CONFIG: " + f);
    }

    // if asked to, write config
    if (outfile != null) {
      try {
        Configuration b = population.getBestConfiguration().getConfiguration();
        ConfigWriter.write(b, outfile);
      } catch (IOException e) {
        System.err.println("ERROR: Cannot write to '" + outfile + "': " + e);
      }
    }
    
    // ask questions, if we're active
    if (active)
      askQuestions(tracker);

    // is there any point in evolving?
    if (active &&
        population.getBestConfiguration().getFNumber() ==
        population.getWorstConfiguration().getFNumber())
      // all configurations rated equally, so we have no idea which
      // ones are best. leaving the population alone until we learn
      // more.
      return;
    
    // produce next generation
    int size = pop.size();
    List<GeneticConfiguration> nextgen = new ArrayList(size);
    for (GeneticConfiguration cfg : pop.subList(0, (int) (size * 0.02)))
      nextgen.add(new GeneticConfiguration(cfg));
    for (GeneticConfiguration cfg : pop.subList(0, (int) (size * 0.03)))
      nextgen.add(new GeneticConfiguration(cfg));
    for (GeneticConfiguration cfg : pop.subList(0, (int) (size * 0.25)))
      nextgen.add(new GeneticConfiguration(cfg));
    for (GeneticConfiguration cfg : pop.subList(0, (int) (size * 0.25)))
      nextgen.add(new GeneticConfiguration(cfg));
    for (GeneticConfiguration cfg : pop.subList((int) (size * 0.25), (int) (size * 0.7)))
      nextgen.add(new GeneticConfiguration(cfg));
    
    if (nextgen.size() > size)
      nextgen = nextgen.subList(0, size);

    for (GeneticConfiguration cfg : nextgen)
      if (Math.random() <= 0.75)
        cfg.mutate();
      else
        cfg.mateWith(population.pickRandomConfig());
    
    population.setNewGeneration(nextgen);
  }

  /**
   * Evaluates the given configuration, storing the score on the object.
   * @param config The configuration to evaluate.
   * @param testdb The link database to test against.
   * @param listener A match listener to register on the processor. Can
   *                 be null.
   * @return The F-number of the configuration.
   */
  private double evaluate(GeneticConfiguration config,
                          LinkDatabase testdb,
                          MatchListener listener) {
    System.out.println(config);

    Configuration cconfig = config.getConfiguration();
    Processor proc = new Processor(cconfig, database);
    TestFileListener eval = new TestFileListener(testdb, cconfig, false,
                                                 proc, false, false);
    eval.setQuiet(true);
    eval.setPessimistic(!active); // active learning requires optimism to work
    proc.addMatchListener(eval);
    if (listener != null)
      proc.addMatchListener(listener);
    if (cconfig.isDeduplicationMode())
      proc.linkRecords(cconfig.getDataSources());
    else
      proc.linkRecords(cconfig.getDataSources(2), false);

    return eval.getFNumber();
  }

  public GeneticConfiguration getBestConfiguration() {
    return population.getBestConfiguration();
  }

  public GeneticPopulation getPopulation() {
    return population;
  }

  private void askQuestions(ExemplarsTracker tracker) {
    int count = 0;
    for (Pair pair : tracker.getExemplars()) {
      if (testdb.inferLink(pair.id1, pair.id2) != null)
        continue; // we already know the answer

      System.out.println();
      Record r1 = database.findRecordById(pair.id1);
      if (r1 == null)
        r1 = secondary.get(pair.id1);
      Record r2 = database.findRecordById(pair.id2);
      PrintMatchListener.prettyCompare(r1, r2, (double) pair.counter,
                                       "Possible match", 
                                       config.getProperties());
      
      LinkKind kind = oracle.getLinkKind(pair.id1, pair.id2);
      Link link = new Link(pair.id1, pair.id2, LinkStatus.ASSERTED, kind);
      testdb.assertLink(link);

      count++;
      if (count == questions)
        break;
    }
  }

  private String getid(Record r) {
    for (String propname : r.getProperties())
      if (config.getPropertyByName(propname).isIdProperty())
        return r.getValue(propname);
    return null;
  }

  // this one tries to find correct matches
  static class FindCorrectComparator implements Comparator<Pair> {
    public int compare(Pair p1, Pair p2) {
      // puts the one with the highest count first
      return p2.counter - p1.counter;
    }
  }

  // this one tries to find the matches with the most information, by
  // picking the ones there is most disagreement on
  class DisagreementComparator implements Comparator<Pair> {
    public int compare(Pair p1, Pair p2) {
      int size = population.size();
      return getScore(p2) - getScore(p1);
    }

    private int getScore(Pair pair) {
      int size = population.size();
      return (size - pair.counter) * (size - (size - pair.counter));
    }
  }
}