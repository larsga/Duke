
package no.priv.garshol.duke.genetic;

import no.priv.garshol.duke.Configuration;

/**
 * Sets the threshold.
 */
public class ThresholdAspect extends FloatAspect {

  public void setRandomly(GeneticConfiguration cfg, double float_drift_range) {
    Configuration config = cfg.getConfiguration();
    double new_value = drift(config.getThreshold(), 1.0, 0.0, 
                             float_drift_range);
    config.setThreshold(new_value);
  }

  public void setFromOther(GeneticConfiguration cfg1,
                           GeneticConfiguration cfg2) {
    Configuration config = cfg1.getConfiguration();
    Configuration other = cfg2.getConfiguration();

    config.setThreshold(other.getThreshold());
  }

  
}
