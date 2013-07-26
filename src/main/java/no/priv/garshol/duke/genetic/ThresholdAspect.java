
package no.priv.garshol.duke.genetic;

import no.priv.garshol.duke.Configuration;

/**
 * Sets the threshold.
 */
public class ThresholdAspect extends Aspect {

  public void setRandomly(Configuration config) {
    config.setThreshold(Math.random());
  }

  public void setFromOther(Configuration config, Configuration other) {
    config.setThreshold(other.getThreshold());
  }
  
}