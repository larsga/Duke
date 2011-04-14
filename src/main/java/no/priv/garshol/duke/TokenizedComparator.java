
package no.priv.garshol.duke;

/**
 * A comparator which breaks values to be tokenized up into smaller
 * components, and then compares the two sets of components.
 */
public class TokenizedComparator implements Comparator {

  public boolean isTokenized() {
    return true;
  }
  
  public double compare(String v1, String v2) {
    if (v1.equals(v2))
      return 1.0;

    String[] t1 = StringUtils.split(v1); // FIXME: split on more chars?
    String[] t2 = StringUtils.split(v2);

    // m: tokens occurring in both
    // n: tokens occurring in only one

    // possible formulas:
    // (1) m/(m+n)

    // results
    // data           (1)
    // a ~ b          0
    // a b ~ a        0.5
    // a b ~ a c      0.33
    // a b ~ c d      0
    // a b c ~ a b d  0.5
    return 0.0;
  }
  
}