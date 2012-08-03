
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.utils.StringUtils;

/**
 * An implementation of the Jaccard index using exact matching by
 * default, but can be overridden to use any sub-comparator.
 */
public class JaccardIndexComparator implements Comparator {
  private Comparator subcomp;
  
  public JaccardIndexComparator() {
    this.subcomp = new ExactComparator();
  }

  public void setComparator(Comparator comp) {
    this.subcomp = comp;
  }
  
  public boolean isTokenized() {
    return true;
  }

  public double compare(String s1, String s2) {
    if (s1.equals(s2))
      return 1.0;

    // tokenize
    String[] t1 = StringUtils.split(s1);
    String[] t2 = StringUtils.split(s2);

    // FIXME: we assume t1 and t2 do not have internal duplicates
    
    // ensure that t1 is shorter than or same length as t2
    if (t1.length > t2.length) {
      String[] tmp = t2;
      t2 = t1;
      t1 = tmp;
    }
    
    // find best matches for each token in t1
    double intersection = 0;
    double union = t1.length + t2.length;
    for (int ix1 = 0; ix1 < t1.length; ix1++) {
      double highest = 0;
      for (int ix2 = 0; ix2 < t2.length; ix2++)
        highest = Math.max(highest, subcomp.compare(t1[ix1], t2[ix2]));

      // INV: the best match for t1[ix1] in t2 is has similarity highest
      intersection += highest;
      union -= highest; // we reduce the union by this similarity
    }

    return intersection / union;
  }
}