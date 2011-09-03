
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;

/**
 * Comparator which compares two values exactly. It returns 1.0 if
 * they are equal, and 0.0 if they are different.
 */
public class ExactComparator implements Comparator {

  public boolean isTokenized() {
    return false;
  }
  
  public double compare(String v1, String v2) {
    return v1.equals(v2) ? 1.0 : 0.0;
  }
  
}