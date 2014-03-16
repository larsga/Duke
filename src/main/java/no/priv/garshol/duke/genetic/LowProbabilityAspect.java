
package no.priv.garshol.duke.genetic;

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Configuration;

/**
 * Sets the low probability.
 */
public class LowProbabilityAspect extends FloatAspect {
  private Property prop;

  public LowProbabilityAspect(Property prop) {
    this.prop = prop;
  }

  public void setRandomly(GeneticConfiguration cfg) {
    Configuration config = cfg.getConfiguration();
    Property p = config.getPropertyByName(prop.getName());
    double new_value = drift(config.getThreshold(), 0.5, 0.0);
    p.setLowProbability(new_value);
  }

  public void setFromOther(GeneticConfiguration cfg1,
                           GeneticConfiguration cfg2) {
    Configuration config = cfg1.getConfiguration();
    Configuration other = cfg2.getConfiguration();

    Property p1 = config.getPropertyByName(prop.getName());
    Property p2 = other.getPropertyByName(prop.getName());
    p1.setLowProbability(p2.getLowProbability());
  }
  
}
