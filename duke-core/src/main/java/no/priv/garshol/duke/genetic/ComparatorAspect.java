
package no.priv.garshol.duke.genetic;

import java.util.List;
import java.util.ArrayList;

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.utils.ObjectUtils;

/**
 * Sets the comparator.
 */
public class ComparatorAspect extends Aspect {
  private Property prop;
  private List<Comparator> comparators;

  public ComparatorAspect(Property prop, List<Comparator> comparators) {
    this.prop = prop;
    this.comparators = comparators;
  }

  public void setRandomly(GeneticConfiguration cfg) {
    Configuration config = cfg.getConfiguration();
    Property p = config.getPropertyByName(prop.getName());
    p.setComparator(comparators.get((int) (comparators.size() * Math.random())));
  }

  public void setFromOther(GeneticConfiguration cfg1,
                           GeneticConfiguration cfg2) {
    Configuration config = cfg1.getConfiguration();
    Configuration other = cfg2.getConfiguration();

    Property p1 = config.getPropertyByName(prop.getName());
    Property p2 = other.getPropertyByName(prop.getName());
    p1.setComparator(p2.getComparator());
  }
}