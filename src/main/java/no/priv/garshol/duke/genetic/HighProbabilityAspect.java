
package no.priv.garshol.duke.genetic;

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Configuration;

/**
 * Sets the high probability.
 */
public class HighProbabilityAspect extends Aspect {
  private Property prop;

  public HighProbabilityAspect(Property prop) {
    this.prop = prop;
  }

  public void setRandomly(Configuration config) {
    Property p = config.getPropertyByName(prop.getName());
    p.setHighProbability(0.5 + (Math.random() / 2.0));
  }

  public void setFromOther(Configuration config, Configuration other) {
    Property p1 = config.getPropertyByName(prop.getName());
    Property p2 = other.getPropertyByName(prop.getName());
    p1.setHighProbability(p2.getHighProbability());
  }
}