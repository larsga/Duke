
package no.priv.garshol.duke.comparators;

import java.util.Set;
import java.util.HashSet;
import no.priv.garshol.duke.Comparator;

// FIXME: add dice coefficient
// FIXME: add jaccard coefficient
// FIXME: add positional q-grams
// FIXME: add first/last-character extension

/**
 * An implementation of q-grams comparison that can tokenize a few
 * different ways, and also use a couple different formulas to compute
 * the final score. The default is using basic q-grams and q-gram
 * overlap.
 */
public class QGramComparator implements Comparator {
  
  public QGramComparator() {
  }
  
  public boolean isTokenized() {
    return true;
  }

  public double compare(String s1, String s2) {
    if (s1.equals(s2))
      return 1.0;

    Set<String> q1 = qgrams(s1);
    Set<String> q2 = qgrams(s2);

    if (q1.isEmpty() || q2.isEmpty())
      return 0.0; // division will fail

    int common = 0;
    for (String gram : q1)
      if (q2.contains(gram))
        common++;

    return overlap(common, q1, q2);
  }

  /**
   * Produces basic q-grams, so that 'gail' -> 'ga', 'ai', 'il'.
   */
  public Set<String> qgrams(String s) {
    int q = 2;

    Set<String> grams = new HashSet();
    for (int ix = 0; ix < s.length() - q + 1; ix++)
      grams.add(s.substring(ix, ix + q));

    return grams;
  }

  public double overlap(int common, Set<String> q1, Set<String> q2) {
    return (double) common / Math.min((double) q1.size(), (double) q2.size());
  }
}