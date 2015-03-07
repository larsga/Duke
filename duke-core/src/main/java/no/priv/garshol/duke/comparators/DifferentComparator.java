
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;

/**
 * A comparator which returns 0.0 if two values are exactly equal, and
 * 1.0 if they are different. The inverse of ExactComparator.
 */
public class DifferentComparator implements Comparator {

  public boolean isTokenized() {
    return false;
  }
  
  public double compare(String v1, String v2) {
    return v1.equals(v2) ? 0.0 : 1.0;
  }
  
}