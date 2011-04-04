
package no.priv.garshol.duke;

/**
 * An operator which compares two values exactly.
 */
public class ExactComparator implements Comparator {

  public boolean isTokenized() {
    return false;
  }
  
  public double compare(String v1, String v2) {
    return v1.equals(v2) ? 1.0 : 0.0;
  }
  
}