
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
  private static List<Comparator> comparators;

  public ComparatorAspect(Property prop) {
    this.prop = prop;
  }

  public void setRandomly(Configuration config) {
    Property p = config.getPropertyByName(prop.getName());
    p.setComparator(comparators.get((int) (comparators.size() * Math.random())));
  }

  public void setFromOther(Configuration config, Configuration other) {
    Property p1 = config.getPropertyByName(prop.getName());
    Property p2 = other.getPropertyByName(prop.getName());
    p1.setComparator(p2.getComparator());
  }

  // static initialization block
  static {
    String PKG = "no.priv.garshol.duke.comparators.";
    String[] compnames = new String[] {
      "DiceCoefficientComparator",
      "DifferentComparator",
      "ExactComparator",
      "JaroWinkler",
      "JaroWinklerTokenized",
      "Levenshtein",
      "NumericComparator",
      "PersonNameComparator",
      "SoundexComparator",
      "WeightedLevenshtein",
      "NorphoneComparator",
      "MetaphoneComparator",
      "QGramComparator",
      "GeopositionComparator",
      "LongestCommonSubstring",
    };

    comparators = new ArrayList();
    for (int ix = 0; ix < compnames.length; ix++)
      comparators.add((Comparator)ObjectUtils.instantiate(PKG + compnames[ix]));
 }
}