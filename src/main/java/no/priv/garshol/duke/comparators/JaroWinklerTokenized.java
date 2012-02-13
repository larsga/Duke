
package no.priv.garshol.duke.comparators;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.utils.StringUtils;

/**
 * A tokenized approach to string similarity, based on Jaccard
 * equivalence and the Jaro-Winkler metric.
 *
 * FIXME: Do we actually need this, or is DiceCoefficientComparator
 * better?  I guess Dice probably is better. However, the code for not
 * allowing same token to be matched twice is unique to this comparator.
 * Should we reuse in Dice, or just support more methods than just Dice?
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
    
    // compute all comparisons
    List<Match> matches = new ArrayList(t1.length * t2.length);    
    for (int ix1 = 0; ix1 < t1.length; ix1++)
      for (int ix2 = 0; ix2 < t2.length; ix2++)
        matches.add(new Match(JaroWinkler.similarity(t1[ix1], t2[ix2]),
                              ix1, ix2));

    // sort
    Collections.sort(matches);

    // now pick the best matches, never allowing the same token to be
    // included twice. we mark a token as used by nulling it in t1|t2.
    double sum = 0.0;
    for (Match m : matches) {
      if (t1[m.ix1] != null && t2[m.ix2] != null) {
        sum += m.score;
        t1[m.ix1] = null;
        t2[m.ix2] = null;
      }
    }

    return sum / t1.length;
  }

  static class Match implements Comparable {
    double score;
    int ix1;
    int ix2;

    public Match(double score, int ix1, int ix2) {
      this.score = score;
      this.ix1 = ix1;
      this.ix2 = ix2;
    }

    public int compareTo(Object other) {
      if (!(other instanceof Match))
        return -1;

      double oscore = ((Match) other).score;
      if (score < oscore)
        return 1;
      else if (score > oscore)
        return -1;
      else
        return 0;
    }
  }

  // THE OLD CODE
  // public double compare(String s1, String s2) {
  //   if (s1.equals(s2))
  //     return 1.0;

  //   // tokenize
  //   String[] t1 = StringUtils.split(s1);
  //   String[] t2 = StringUtils.split(s2);

  //   // ensure that t1 is shorter than or same length as t2
  //   if (t1.length > t2.length) {
  //     String[] tmp = t2;
  //     t2 = t1;
  //     t1 = tmp;
  //   }
    
  //   // find best matches for each token in t1
  //   double sum = 0;
  //   for (int ix1 = 0; ix1 < t1.length; ix1++) {
  //     double highest = 0;
  //     for (int ix2 = 0; ix2 < t2.length; ix2++)
  //       highest = Math.max(highest, JaroWinkler.similarity(t1[ix1], t2[ix2]));
  //     sum += highest;
  //   }

  //   return sum / t1.length;
  // }
}