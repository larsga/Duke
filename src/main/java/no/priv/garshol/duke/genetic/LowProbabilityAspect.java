
package no.priv.garshol.duke.genetic;

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Configuration;

/**
 * Sets the low probability.
 */
public class LowProbabilityAspect extends Aspect {
  private Property prop;

  public LowProbabilityAspect(Property prop) {
    this.prop = prop;
  }

  public void setRandomly(Configuration config) {
    Property p = config.getPropertyByName(prop.getName());
    p.setLowProbability(Math.random() / 2.0);
  }

  public void setFromOther(Configuration config, Configuration other) {
    Property p1 = config.getPropertyByName(prop.getName());
    Property p2 = other.getPropertyByName(prop.getName());
    p1.setLowProbability(p2.getLowProbability());
  }
  
}