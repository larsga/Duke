
package no.priv.garshol.duke.genetic;

/**
 * Represents an aspect of a Configuration that might be changed by
 * the genetic algorithm.
 */
public abstract class Aspect {

  /**
   * Randomly modify this aspect of the configuration.
   */
  public abstract void setRandomly(GeneticConfiguration config);

  /**
   * Set this aspect of the configuration to be the same as that of
   * the other configuration.
   */
  public abstract void setFromOther(GeneticConfiguration config,
                                    GeneticConfiguration other);
}
