
package no.priv.garshol.duke.genetic;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import no.priv.garshol.duke.Configuration;

/**
 * Keeps track of the population.
 */
public class GeneticPopulation {
  private GeneticConfiguration config; // this is the original config
  private List<GeneticConfiguration> population;
  private int size;

  public GeneticPopulation(Configuration config) {
    this.config = new GeneticConfiguration(config);
    this.size = 100;
  }
  
  /**
   * Creates the initial population.
   */
  public void create() {
    population = new ArrayList(size);
    for (int ix = 0; ix < size; ix++)
      population.add(config.makeRandomCopy());
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
    return null;
  }

  /**
   * Returns a random configuration.
   */
  public GeneticConfiguration pickRandomConfig() {
    return population.get((int) Math.random() * population.size());
  }

  /**
   * Returns the size of the population.
   */
  public int size() {
    return size;
  }
}
