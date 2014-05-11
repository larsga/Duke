
package no.priv.garshol.duke.genetic;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import no.priv.garshol.duke.Configuration;

/**
 * Keeps track of the population.
 */
public class GeneticPopulation {
  private Configuration config; // this is the original config
  private List<GeneticConfiguration> population;
  private int size;
  private int mutation_rate;
  private double recombination_rate;
  private int copies_of_original;
  private boolean evolve_comparators; // should we evolve comparators?

  public GeneticPopulation(Configuration config) {
    this.config = config;
    this.size = 100;
    this.mutation_rate = -1;
    this.recombination_rate = -1.0;
    this.copies_of_original = 0;
    this.evolve_comparators = true;
  }

  /**
   * Creates the initial population.
   */
  public void create() {
    GeneticConfiguration cfg =
      new GeneticConfiguration(config, mutation_rate, recombination_rate,
                               evolve_comparators);
    population = new ArrayList(size);
    int ix = 0;
    for (; ix < copies_of_original; ix++)
      population.add(cfg.makeCopy());
    for (; ix < size; ix++)
      population.add(cfg.makeRandomCopy());
  }

  /**
   * Returns all configurations in the current generation.
   */
  public List<GeneticConfiguration> getConfigs() {
    return population;
  }

  public void setNewGeneration(List<GeneticConfiguration> nextgen) {
    this.population = nextgen;
  }

  /**
   * Sorts the population by their achieved F-numbers.
   */
  public void sort() {
    Collections.sort(population);
    for (int ix = 0; ix < population.size(); ix++)
      population.get(ix).setRank(ix + 1);
  }

  /**
   * Returns the best configuration.
   */
  public GeneticConfiguration getBestConfiguration() {
    return population.get(0);
  }

  /**
   * Returns the worst configuration.
   */
  public GeneticConfiguration getWorstConfiguration() {
    return population.get(population.size() - 1);
  }

  /**
   * Returns a random configuration.
   */
  public GeneticConfiguration pickRandomConfig() {
    return population.get((int) Math.random() * population.size());
  }

  /**
   * Runs a tournament among k individuals to find the most fit
   * individual.
   */
  public GeneticConfiguration runTournament(int k) {
    GeneticConfiguration best = pickRandomConfig();
    for (int ix = 1; ix < k; ix++) {
      GeneticConfiguration candidate = pickRandomConfig();
      if (candidate.getFNumber() > best.getFNumber())
        best = candidate;
    }
    return best;
  }

  /**
   * Sets the size of the population.
   */
  public void setSize(int size) {
    this.size = size;
  }

  public void setMutationRate(int mutation_rate) {
    this.mutation_rate = mutation_rate;
  }

  public void setRecombinationRate(double recombination_rate) {
    this.recombination_rate = recombination_rate;
  }

  public void setEvolveComparators(boolean evolve_comparators) {
    this.evolve_comparators = evolve_comparators;
  }

  public void setCopiesOfOriginal(int copies) {
    this.copies_of_original = copies;
  }

  /**
   * Returns the size of the population.
   */
  public int size() {
    return size;
  }
}
