
package no.priv.garshol.duke.genetic;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.priv.garshol.duke.*;
import no.priv.garshol.duke.matchers.MatchListener;
import no.priv.garshol.duke.matchers.PrintMatchListener;
import no.priv.garshol.duke.matchers.TestFileListener;
import no.priv.garshol.duke.utils.LinkDatabaseUtils;

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
  private boolean quiet; // limit output
  private boolean incomplete; // is test file incomplete?

  private int threads; // parallel threads to run
  private int generations;
  private int questions; // number of questions to ask per iteration
  private boolean sparse; // whether to skip asking questions after some gens
  private int skipgens; // number of generations left to skip
  private int asked; // number of questions asked

  private Collection<Pair> used; // all the pairs we've ever asked about

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
    //testdb.setDoInference(true);
    this.scientific = scientific;
    this.threads = 1;
    this.used = new ArrayList();

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
   * Tells the genetic algorithm not to output more than necessary.
   */
  public void setQuiet(boolean quiet) {
    this.quiet = quiet;
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
   * Sets the number of mutations to perform on each new configuration
   * for each generation. If not set, the algorithm will evolve a
   * mutation rate.
   */
  public void setMutationRate(int mutation_rate) {
    population.setMutationRate(mutation_rate);
  }

  /**
   * Sets the number of recombinations to perform on each new
   * configuration for each generation. 0.75 means there's a 75%
   * chance we do one recombination. 1.75 means we do one for certain,
   * and, with 75% probability do another.
   */
  public void setRecombinationRate(double recombination_rate) {
    population.setRecombinationRate(recombination_rate);
  }

  /**
   * If true, the algorithm will not evolve the comparators, but only
   * the other aspects of the configuration. The default is to evolve
   * comparators, too.
   */
  public void setEvolveComparators(boolean evolve_comparators) {
    population.setEvolveComparators(evolve_comparators);
  }

  /**
   * Sets how many copies of the original configuration to keep in the
   * first generation. The default is 0, meaning the first generation
   * will be entirely random, but with this option you can make the
   * genetic algorithm start from your existing configuration.
   */
  public void setCopiesOfOriginal(int copies) {
    population.setCopiesOfOriginal(copies);
  }

  /**
   * Tells the algorithm whether to assume the test file contains all
   * correct pairs.
   */
  public void setIncompleteTest(boolean incomplete) {
    this.incomplete = incomplete;
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
      if (!quiet)
        System.out.println("===== GENERATION " + gen);
      double best = evolve(gen);
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
    if (!quiet) {
      System.out.println("BEST: " + lbest + " AVERAGE: " + (fsum / pop.size()));
      for (GeneticConfiguration cfg : pop)
        System.out.print(cfg.getFNumber() + " ");
      System.out.println();
    }

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

      if (!quiet) {
        System.out.println("ACTUAL BEST: " + sciencetracker.get(best) +
                           " ACTUAL AVERAGE: " + (fsum / pop.size()));
        System.out.println("AVERAGE DEVIATION: " + (devsum / pop.size()));
        System.out.println("QUESTIONS ASKED: " + used.size());
        System.out.println();
      }
      sciencetracker.clear();
    }

    // if asked to, write config
    if (outfile != null) {
      try {
        Configuration b = population.getBestConfiguration().getConfiguration();
          FileOutputStream fos = new FileOutputStream(outfile);
          ConfigWriter configWriter = new ConfigWriter(fos);
          configWriter.write(b);
          fos.close();
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

  private void produceNextGeneration() {
    // this code uses simple (mu, lambda) evolution. according to the
    // literature tournament selection should be better, but careful
    // experimentation revealed no measurable benefits whatever. the
    // tournament code has therefore been removed.

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
    }

    population.setNewGeneration(nextgen);
  }

  private void evaluateAll(ExemplarsTracker tracker) {
    List<GeneticConfiguration> pop = population.getConfigs();
    for (GeneticConfiguration cfg : pop) {
      if (!quiet)
        System.out.println(cfg);
      double f = evaluate(cfg, tracker);
      if (!quiet)
        System.out.print("  " + f);
      if (f > best) {
        if (!quiet)
          System.out.println("\nNEW BEST! " + f + "\n");
        best = f;
      }
      if (!quiet) {
        if (scientific)
          System.out.println("  (actual: " + sciencetracker.get(cfg) + ")");
        else
          System.out.println();
      }
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

    if (active || incomplete)
      // in active learning the test file is incomplete, so F-number eval
      // should be optimistic. similarly if the test file is known to be
      // incomplete, for whatever reason
      eval.setPessimistic(false);

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
    Filter f = new Filter(tracker.getExemplars());
    while (true) {
      Pair pair = f.getNext();
      if (pair == null)
        break;
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
    for (String propname : r.getProperties()) {
      Property prop = config.getPropertyByName(propname);
      if (prop == null)
        throw new DukeConfigException("Record has property " + propname +
                                      " which is not in configuration");

      if (prop.isIdProperty())
        return r.getValue(propname);
    }
    return null;
  }

  // ----- FILTER

  // this filter is used to weed out questions that are duplicates of
  // questions already asked, but duplicates in a way that's difficult
  // to detect (hence all the code). what it does is explained here:
  // http://www.garshol.priv.no/blog/273.html

  class Filter {
    private List<Pair> exemplars;

    public Filter(List<Pair> exemplars) {
      this.exemplars = exemplars;
      applyFilter();
    }

    public Pair getNext() {
      if (exemplars.isEmpty())
        return null;

      // find the candidate pair with the lowest similarity score with
      // already used pairs
      double bestscore = 2.0;
      Pair thebest = exemplars.get(0); // just in case
      for (Pair candidate : exemplars) {
        if (testdb.inferLink(candidate.id1, candidate.id2) != null)
          continue; // we already know the answer
        double worst = 0.0;

        for (Pair seen : used) {
          double score = compare(candidate, seen);
          if (score > worst)
            worst = score;
        }

        if (worst < bestscore) {
          bestscore = worst;
          thebest = candidate;
        }
      }

      // now we know which one to return
      used.add(thebest);
      exemplars.remove(thebest);
      return thebest;
    }

    // find the n*2 best
    private void applyFilter() {
      List<Pair> chosen = new ArrayList();
      for (int next = 0; chosen.size() < questions * 2 &&
                         next < exemplars.size(); next++) {
        Pair pair = exemplars.get(next);
        if (testdb.inferLink(pair.id1, pair.id2) != null)
          continue; // we already know the answer
        pair.believers = whoThinksThisIsTrue(pair.id1, pair.id2);
        chosen.add(pair);
      }

      exemplars = chosen;
    }

    // we use Jaccard index, which is size of intersection divided by
    // size of union
    private double compare(Pair p1, Pair p2) {
      int intersection = 0;
      int union = 0;
      for (int ix = 0; ix < p1.believers.length; ix++) {
        if (p1.believers[ix] && p2.believers[ix])
          intersection++;
        if (p1.believers[ix] || p2.believers[ix])
          union++;
      }
      return ((double) intersection) / ((double) union);
    }

    private boolean[] whoThinksThisIsTrue(String id1, String id2) {
      Record r1 = database.findRecordById(id1);
      if (r1 == null)
        r1 = secondary.get(id1);
      Record r2 = database.findRecordById(id2);
      if (r2 == null)
        r2 = secondary.get(id2);

      List<GeneticConfiguration> configs = population.getConfigs();
      boolean[] believers = new boolean[configs.size()];
      for (int ix = 0; ix < configs.size(); ix++) {
        Configuration config = configs.get(ix).getConfiguration();
        Processor proc = new Processor(config, database);
        believers[ix] = proc.compare(r1, r2) > config.getThreshold();
      }
      return believers;
    }
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
      double f = cfg.getFNumber();

      if (!quiet) {
        System.out.println(cfg);
        System.out.print("  " + f);
      }
      if (f > best) {
        if (!quiet)
          System.out.println("\nNEW BEST! " + f + "\n");
        best = f;
      }
      if (!quiet) {
        if (scientific)
          System.out.println("  (actual: " + sciencetracker.get(cfg) + ")");
        else
          System.out.println();
      }
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
