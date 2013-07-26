
package no.priv.garshol.duke.genetic;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.DataSource;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.InMemoryLinkDatabase;
import no.priv.garshol.duke.utils.LinkDatabaseUtils;
import no.priv.garshol.duke.matchers.TestFileListener;

/**
 * The class that actually runs the genetic algorithm.
 */
public class GeneticAlgorithm {
  private Configuration config;
  private GeneticPopulation population;
  private Database database;
  private int generations;
  private InMemoryLinkDatabase testdb;
  private double best; // best ever
  
  public GeneticAlgorithm(Configuration config, String testfile)
    throws IOException {
    this.config = config;
    this.population = new GeneticPopulation(config);
    this.generations = 100;
    this.testdb = new InMemoryLinkDatabase();
    testdb.setDoInference(true);
    LinkDatabaseUtils.loadTestFile(testfile, testdb);
  }

  /**
   * Actually runs the genetic algorithm.
   */
  public void run() {
    // first index up all records
    database = config.createDatabase(true);
    for (DataSource src : config.getDataSources()) {
      RecordIterator it = src.getRecords();
      while (it.hasNext())
        database.index(it.next());
    }
    database.commit();
    
    // make first, random population
    population.create();

    // run through the required number of generations
    for (int gen = 0; gen < generations; gen++) {
      System.out.println("===== GENERATION " + gen);
      evolve();
    }
  }

  /**
   * Creates a new generation.
   */
  public void evolve() {
    // evaluate current generation
    List<GeneticConfiguration> pop = population.getConfigs();
    for (GeneticConfiguration cfg : pop) {
      double f = evaluate(cfg);
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
   */
  private double evaluate(GeneticConfiguration config) {
    System.out.println(config);

    Configuration cconfig = config.getConfiguration();
    Processor proc = new Processor(cconfig, database);
    TestFileListener eval = new TestFileListener(testdb, cconfig, false,
                                                 proc, false, false);
    eval.setQuiet(true);
    eval.setPessimistic(true);
    proc.addMatchListener(eval);
    proc.linkRecords(cconfig.getDataSources());  // FIXME: record linkage mode

    config.setFNumber(eval.getFNumber());
    return eval.getFNumber();
  }

  public GeneticConfiguration getBestConfiguration() {
    return population.getBestConfiguration();
  }

  public GeneticPopulation getPopulation() {
    return population;
  }
}