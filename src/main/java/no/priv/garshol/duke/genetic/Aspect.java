
package no.priv.garshol.duke.genetic;

import no.priv.garshol.duke.Configuration;

/**
 * Represents an aspect of a Configuration that might be changed by
 * the genetic algorithm.
 */
public abstract class Aspect {

  /**
   * Randomly modify this aspect of the Configuration.
   */
  public abstract void setRandomly(Configuration config);

  /**
   * Set this aspect of the configuration to be the same as that of
   * the other configuration.
   */
  public abstract void setFromOther(Configuration config, Configuration other);
}