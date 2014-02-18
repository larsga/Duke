
package no.priv.garshol.duke.genetic;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
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
import no.priv.garshol.duke.DukeConfigException;
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
  private Map<GeneticConfiguration, Double> sciencetracker;

  private int threads; // parallel threads to run
  private int generations;
  private int questions; // number of questions to ask per iteration
  private boolean sparse; // whether to skip asking questions after some gens
  private int skipgens; // number of generations left to skip
  private int asked; // number of questions asked

  /**
   * Creates the algorithm.
   * @param testfile Test file to evaluate configs against. If null
   *                 we use active learning instead.
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
    this.threads = 1;

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
      this.sciencetracker = Collections.synchronizedMap(new HashMap());
    }
  }

  /**
   * Sets the number of generations to run the algorithm for. Default
   * 100.
   */
  public void setGenerations(int generations) {
    this.generations = generations;
  }

  /**
   * Sets the size of the population. Default 100.
   */
  public void setPopulation(int population) {
    this.population.setSize(population);
  }

  /**
   * Sets the number of questions to ask per generation in active
   * learning mode. Default 10.
   */
  public void setQuestions(int questions) {
    this.questions = questions;
  }

  /**
   * Set the file to write the best configuration to. The
   * configuration gets written at the end of each generation.
   */
  public void setConfigOutput(String output) {
    this.outfile = output;
  }

  /**
   * Sets the number of threads to run the genetic algorithm in.
   */
  public void setThreads(int threads) {
    this.threads = threads;
  }
  
  public void setActive(boolean active) {
    // basically, if we have a link file, and call this method, what
    // it means is that we'll evaluate in optimistic mode. that is, we
    // assume that there are correct matches that don't exist in the
    // test file
    this.active = active;
  }

  public void setSparse(boolean sparse) {
    this.sparse = sparse;
  }

  /**
   * Sets the file to write user's answers to in active learning mode.
   */
  public void setLinkFile(String linkfile) throws IOException {
    if (scientific || !active || oracle instanceof LinkFileOracle)
      throw new DukeConfigException("Have no use for link file");

    ((ConsoleOracle) oracle).setLinkFile(linkfile);
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
      
    database = config.getDatabase(true);
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
    double prevbest = 0.0;
    int stuck_for = 0; // number of generations f has remained unchanged
    for (int gen = 0; gen < generations; gen++) {
      System.out.println("===== GENERATION " + gen);
      double best = evolve(gen);
      
      // if (best == prevbest) {
      //   stuck_for++;
      //   if (stuck_for > 4) {
      //     // we just blast through and hard-wire the mutation rate of all inds
      //     for (GeneticConfiguration cfg : population.getConfigs())
      //       cfg.setMutationRate(stuck_for - 3); // 5 -> 2, 6 -> 3, ...
      //   }
      // } else {
      //   prevbest = best;
      //   stuck_for = 0;
      // }
    }
  }

  /**
   * Creates a new generation.
   * @param gen_no The number of the generation. The first is 0.
   */
  public double evolve(int gen_no) {
    // evaluate current generation
    ExemplarsTracker tracker = null;
    if (active) {
      // the first time we try to find correct matches so that we're
      // guranteed the algorithm knows about *some* correct matches
      Comparator comparator = gen_no == 0 ?
        new FindCorrectComparator() : new DisagreementComparator();
      tracker = new ExemplarsTracker(config, comparator);
    }
    if (threads == 1)
      evaluateAll(tracker);
    else
      evaluateAllThreaded(tracker);

    population.sort();

    // compute some key statistics
    double fsum = 0.0;
    double lbest = -1.0;
    GeneticConfiguration best = null;
    List<GeneticConfiguration> pop = population.getConfigs();
    for (GeneticConfiguration cfg : pop) {
      fsum += cfg.getFNumber();
      if (cfg.getFNumber() > lbest) {
        lbest = cfg.getFNumber();
        best = cfg;
      }
    }
    System.out.println("BEST: " + lbest + " AVERAGE: " + (fsum / pop.size()));
    for (GeneticConfiguration cfg : pop)
      System.out.print(cfg.getFNumber() + " ");
    System.out.println();
    
    // ask questions, if we're active
    if (active && skipgens == 0) {
      askQuestions(tracker);
      if (sparse) {
        if (gen_no > 9)
          skipgens = 3; // ask every fourth generation after 10th gen
        else if (gen_no > 1)
          skipgens = 1; // ask every second generation after the first two
      }
    } else if (skipgens > 0) // if we skipped asking, make note of that
      skipgens--;

    // in scientific mode, summarize true statistics for this generation
    if (scientific) {
      double devsum = 0.0;
      fsum = 0.0;
      lbest = -1.0;
      for (GeneticConfiguration cfg : pop) {
        double real = sciencetracker.get(cfg);
        devsum += Math.abs(cfg.getFNumber() - real);
        fsum += real;
        if (real > lbest)
          lbest = real;
      }
      
      System.out.println("ACTUAL BEST: " + sciencetracker.get(best) +
                         " ACTUAL AVERAGE: " + (fsum / pop.size()));
      System.out.println("AVERAGE DEVIATION: " + (devsum / pop.size()));
      System.out.println("QUESTIONS ASKED: " + asked);
      System.out.println();
      sciencetracker.clear();
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

    // is there any point in evolving?
    if (active &&
        population.getBestConfiguration().getFNumber() ==
        population.getWorstConfiguration().getFNumber())
      // all configurations rated equally, so we have no idea which
      // ones are best. leaving the population alone until we learn
      // more.
      return lbest;
    
    // produce next generation
    produceNextGeneration();
    return lbest;
  }

  // trying out tournament selection
  private void produceNextGeneration_() {
    int t_size = 5; // bigger tournament
    int size = population.size();
    List<GeneticConfiguration> nextgen = new ArrayList(size);
    for (int ix = 0; ix < size; ix++) {
      // pick breeding individual by tournament
      GeneticConfiguration cfg = population.runTournament(t_size);
      cfg = new GeneticConfiguration(cfg);

      // do recombination
      double rr = cfg.getRecombinationRate();
      while (rr > Math.random()) {
        cfg.mateWith(population.runTournament(t_size));
        rr -= 1.0;
      }

      // mutate
      for (int i = 0; i < cfg.getMutationRate(); i++)
        cfg.mutate();

      nextgen.add(cfg);
    }
    population.setNewGeneration(nextgen);
  }
  
  private void produceNextGeneration() {
    List<GeneticConfiguration> pop = population.getConfigs();
    int size = pop.size();
    List<GeneticConfiguration> nextgen = new ArrayList(size);
    for (GeneticConfiguration cfg : pop.subList(0, (int) (size * 0.02)))
      nextgen.add(new GeneticConfiguration(cfg));
    for (GeneticConfiguration cfg : pop.subList(0, (int) (size * 0.03)))
      nextgen.add(new GeneticConfiguration(cfg));
    int start = (int) (size * 0.25);
    for (GeneticConfiguration cfg : pop.subList(0, start))
      nextgen.add(new GeneticConfiguration(cfg));
    for (GeneticConfiguration cfg : pop.subList(0, start))
      nextgen.add(new GeneticConfiguration(cfg));
    int remaining = pop.size() - nextgen.size(); // avoids rounding errors
    for (GeneticConfiguration cfg : pop.subList(start, start + remaining))
      nextgen.add(new GeneticConfiguration(cfg));
    
    if (nextgen.size() > size)
      nextgen = nextgen.subList(0, size);

    for (GeneticConfiguration cfg : nextgen) {
      double rr = cfg.getRecombinationRate();
      while (rr > Math.random()) {
        cfg.mateWith(population.pickRandomConfig());
        rr -= 1.0;
      }

      for (int ix = 0; ix < cfg.getMutationRate(); ix++)
        cfg.mutate();
        
      // if (Math.random() <= 0.75)
      //   cfg.mutate();
      // else
      //   cfg.mateWith(population.pickRandomConfig());
    }
    
    population.setNewGeneration(nextgen);
  }  

  private void evaluateAll(ExemplarsTracker tracker) {
    List<GeneticConfiguration> pop = population.getConfigs();
    for (GeneticConfiguration cfg : pop) {
      System.out.println(cfg);
      double f = evaluate(cfg, tracker);
      System.out.print("  " + f);
      if (f > best) {
        System.out.println("\nNEW BEST!\n");
        best = f;
      }
      if (scientific)
        System.out.println("  (actual: " + sciencetracker.get(cfg) + ")");
      else
        System.out.println();
    }
  }

  private void evaluateAllThreaded(ExemplarsTracker tracker) {
    WorkManager mgr = new WorkManager(population.getConfigs());

    // start threads
    WorkerThread[] workers = new WorkerThread[threads];
    for (int ix = 0; ix < threads; ix++) {
      workers[ix] = new WorkerThread(tracker, mgr, ix);
      workers[ix].start();
    }

    // wait for threads to finish
    try {
      for (int ix = 0; ix < workers.length; ix++)
        workers[ix].join();
    } catch (InterruptedException e) {
      // argh
    }
  }

  /**
   * Evaluates the given configuration, storing the score on the object.
   * @param config The configuration to evaluate.
   * @param listener A match listener to register on the processor. Can
   *                 be null.
   * @return The F-number of the configuration.
   */
  private double evaluate(GeneticConfiguration config,
                          MatchListener listener) {
    Configuration cconfig = config.getConfiguration();
    Processor proc = new Processor(cconfig, database);
    TestFileListener eval = makeEval(cconfig, testdb, proc);
    eval.setPessimistic(!active); // active learning requires optimism to work
    proc.addMatchListener(eval);
    TestFileListener seval = null;
    if (scientific) {
      seval = makeEval(cconfig, ((LinkFileOracle) oracle).getLinkDatabase(),
                       proc);
      seval.setPessimistic(true);
      proc.addMatchListener(seval);
    }
    if (listener != null)
      proc.addMatchListener(listener);
    if (cconfig.isDeduplicationMode())
      proc.linkRecords(cconfig.getDataSources());
    else
      proc.linkRecords(cconfig.getDataSources(2), false);

    if (seval != null)
      sciencetracker.put(config, seval.getFNumber());
    
    config.setFNumber(eval.getFNumber());
    return eval.getFNumber();
  }

  private TestFileListener makeEval(Configuration cfg, LinkDatabase testdb,
                                    Processor proc) {
    TestFileListener eval = new TestFileListener(testdb, cfg, false,
                                                 proc, false, false);
    eval.setQuiet(true);
    return eval;
  }

  /**
   * Returns the best configuration we've seen so far.
   */
  public GeneticConfiguration getBestConfiguration() {
    return population.getBestConfiguration();
  }

  /**
   * Returns the current population.
   */ 
  public GeneticPopulation getPopulation() {
    return population;
  }

  private void askQuestions(ExemplarsTracker tracker) {
    int count = 0;
    for (Pair pair : tracker.getExemplars()) {
      if (testdb.inferLink(pair.id1, pair.id2) != null)
        continue; // we already know the answer

      Record r1 = database.findRecordById(pair.id1);
      if (r1 == null)
        r1 = secondary.get(pair.id1);
      Record r2 = database.findRecordById(pair.id2);
      System.out.println();
      PrintMatchListener.prettyCompare(r1, r2, (double) pair.counter,
                                       "Possible match", 
                                       config.getProperties());
      
      LinkKind kind = oracle.getLinkKind(pair.id1, pair.id2);
      Link link = new Link(pair.id1, pair.id2, LinkStatus.ASSERTED, kind, 1.0);
      testdb.assertLink(link);

      count++;
      if (count == questions)
        break;
    }
    asked += count;
  }

  private String getid(Record r) {
    for (String propname : r.getProperties())
      if (config.getPropertyByName(propname).isIdProperty())
        return r.getValue(propname);
    return null;
  }

  // ----- COMPARATORS
  
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

  // ----- THREAD HANDLING

  class WorkManager {
    private List<GeneticConfiguration> pop;
    private int next;

    public WorkManager(List<GeneticConfiguration> pop) {
      this.pop = pop;
    }

    public synchronized GeneticConfiguration getNextConfig() {
      if (next < pop.size())
        return pop.get(next++);
      else
        return null;
    }

    public synchronized void evaluated(GeneticConfiguration cfg) {
      System.out.println(cfg);
      double f = cfg.getFNumber();
      System.out.print("  " + f);
      if (f > best) {
        System.out.println("\nNEW BEST!\n");
        best = f;
      }
      if (scientific)
        System.out.println("  (actual: " + sciencetracker.get(cfg) + ")");
      else
        System.out.println();
    }
  }

  class WorkerThread extends Thread {
    private WorkManager mgr;
    private ExemplarsTracker tracker;

    public WorkerThread(ExemplarsTracker tracker, WorkManager mgr,
                        int threadno) {
      super("WorkerThread " + threadno);
      this.mgr = mgr;
      this.tracker = tracker;
    }

    public void run() {
      GeneticConfiguration cfg = mgr.getNextConfig();
      while (cfg != null) {
        evaluate(cfg, tracker);
        mgr.evaluated(cfg);
        cfg = mgr.getNextConfig();
      }
    }
  }  
}
