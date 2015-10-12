
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;

/**
 * Comparator which compares two values numerically. The similarity is
 * the ratio of the smaller number to the greater number, if both
 * numbers are either negative or positive. If one is negative and the
 * other positive, the similarity is 0.0.
 */
public class NumericComparator implements Comparator {
  private double minratio;

  public boolean isTokenized() {
    return false;
  }

  public void setMinRatio(double minratio) {
    this.minratio = minratio;
  }

  public double compare(String v1, String v2) {
    double d1;
    double d2;
    try {
      d1 = Double.parseDouble(v1);
      d2 = Double.parseDouble(v2);
    } catch (NumberFormatException e) {
      return 0.5; // we just ignore this. whether it's wise I'm not sure
    }

    // if they're both zero, they're equal
    if (d1 == 0.0 && d2 == 0.0)
      return 1.0;

    // if both are negative, flip the signs
    if (d1 < 0.0 && d2 < 0.0) {
      d1 *= -1.0;
      d2 *= -1.0;
    }

    if (d2 < d1) {
      double tmp = d2;
      d2 = d1;
      d1 = tmp;
    }

    double ratio = d1 / d2;
    if (ratio < minratio)
      return 0.0;
    else
      return ratio;
  }

}
