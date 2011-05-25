
package no.priv.garshol.duke;

/**
 * A tokenized approach to string similarity, based on Jaccard
 * equivalence and the Jaro-Winkler metric.
 */
public class JaroWinklerTokenized implements Comparator {

  public boolean isTokenized() {
    return true;
  }

  public double compare(String s1, String s2) {
    if (s1.equals(s2))
      return 1.0;

    // tokenize
    String[] t1 = StringUtils.split(s1);
    String[] t2 = StringUtils.split(s2);

    // ensure that t1 is shorter than or same length as t2
    if (t1.length > t2.length) {
      String[] tmp = t2;
      t2 = t1;
      t1 = tmp;
    }
    
    // find best matches for each token in t1
    double sum = 0;
    for (int ix1 = 0; ix1 < t1.length; ix1++) {
      double highest = 0;
      for (int ix2 = 0; ix2 < t2.length; ix2++)
        highest = Math.max(highest, JaroWinkler.similarity(t1[ix1], t2[ix2]));
      sum += highest;
    }

    return sum / t1.length;
  }
}