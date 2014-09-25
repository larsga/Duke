
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;

/**
 * An implementation of the Levenshtein distance metric that uses
 * weights, so that not all editing operations are considered equal.
 * Useful explanation: http://www.let.rug.nl/kleiweg/lev/levenshtein.html
 */
public class WeightedLevenshtein implements Comparator {
  private WeightEstimator estimator;

  public WeightedLevenshtein() {
    this.estimator = new DefaultWeightEstimator();
  }

  public double compare(String s1, String s2) {
    // if the strings are equal we can stop right here.
    if (s1.equals(s2))
      return 1.0;

    // we couldn't shortcut, so now we go ahead and compute the full
    // matrix
    int len = Math.min(s1.length(), s2.length());
    double dist = distance(s1, s2, estimator);
    if (dist > len)
      // because of weights it's possible for the distance to be
      // greater than the length. if so, we return zero rather than a
      // negative distance.
      return 0.0;
    return 1.0 - (dist / ((double) len));
  }

  public boolean isTokenized() {
    return true;
  }

  public void setEstimator(WeightEstimator estimator) {
    this.estimator = estimator;
  }

  public WeightEstimator getEstimator() {
    return estimator;
  }

  public static double distance(String s1, String s2, WeightEstimator weight) {
    int s1len = s1.length();
    if (s1len == 0)
      return estimateCharacters(s2, weight);
    if (s2.length() == 0)
      return estimateCharacters(s1, weight);

    // we use a flat array for better performance. we address it by
    // s1ix + s1len * s2ix. this modification improves performance
    // by about 30%, which is definitely worth the extra complexity.
    double[] matrix = new double[(s1len + 1) * (s2.length() + 1)];
    for (int col = 0; col <= s2.length(); col++)
      matrix[col * s1len] = col;
    for (int row = 0; row <= s1len; row++)
      matrix[row] = row;

    for (int ix1 = 0; ix1 < s1len; ix1++) {
      char ch1 = s1.charAt(ix1);
      for (int ix2 = 0; ix2 < s2.length(); ix2++) {
        double cost;
        char ch2 = s2.charAt(ix2);
        if (ch1 == ch2)
          cost = 0;
        else
          cost = weight.substitute(ix1, ch1, s2.charAt(ix2));

        double left = matrix[ix1 + ((ix2 + 1) * s1len)] +
                      weight.delete(ix1, ch1);
        double above = matrix[ix1 + 1 + (ix2 * s1len)] +
                      weight.insert(ix1, ch2);
        double aboveleft = matrix[ix1 + (ix2 * s1len)] + cost;
        matrix[ix1 + 1 + ((ix2 + 1) * s1len)] =
          Math.min(left, Math.min(above, aboveleft));
      }
    }

    // for (int ix1 = 0; ix1 <= s1len; ix1++) {
    //   for (int ix2 = 0; ix2 <= s2.length(); ix2++) {
    //     System.out.print(matrix[ix1 + (ix2 * s1len)] + " ");
    //   }
    //   System.out.println();
    // }

    return matrix[s1len + (s2.length() * s1len)];
  }

  // /**
  //  * Optimized version of the Wagner & Fischer algorithm that only
  //  * keeps a single column in the matrix in memory at a time. It
  //  * implements the simple cutoff, but otherwise computes the entire
  //  * matrix.
  //  */
  // public static double compactDistance(String s1, String s2,
  //                                      WeightEstimator weight) {
  //   int s1len = s1.length();
  //   if (s1len == 0)
  //     return (double) estimateCharacters(s2, weight);
  //   if (s2.length() == 0)
  //     return (double) estimateCharacters(s1, weight);

  //   // the maximum edit distance there is any point in reporting.
  //   double maxdist = (double) Math.min(s1.length(), s2.length()) / 2;

  //   // we allocate just one column instead of the entire matrix, in
  //   // order to save space.  this also enables us to implement the
  //   // optimized algorithm somewhat faster, and without recursion.
  //   // the first cell is always the virtual first row.
  //   double[] column = new double[s1len + 1];

  //   // first we need to fill in the initial column. we use a separate
  //   // loop for this, because in this case our basis for comparison is
  //   // not the previous column, but virtual first column. also, logic
  //   // is a little different, since we start from the diagonal.
  //   int ix2 = 0;
  //   char ch2 = s2.charAt(ix2);
  //   column[0] = 1.0; // virtual first row
  //   for (int ix1 = 1; ix1 <= s1len; ix1++) {
  //     double cost = s1.charAt(ix1 - 1) == ch2 ?
  //       0 : weight.substitute(ix1, s1.charAt(ix1 - 1), ch2);

  //     // Lowest of three: above (column[ix1 - 1]), aboveleft: ix1 - 1,
  //     // left: ix1. Latter cannot possibly be lowest, so is
  //     // ignored.
  //     column[ix1] = Math.min(column[ix1 - 1], ix1 - 1) + cost;
  //   }

  //   // okay, now we have an initialized first column, and we can
  //   // compute the rest of the matrix.
  //   double above = 0;
  //   for (ix2 = 1; ix2 < s2.length(); ix2++) {
  //     ch2 = s2.charAt(ix2);
  //     above = ix2 + 1; // virtual first row

  //     double smallest = Double.MAX_VALUE;
  //     for (int ix1 = 1; ix1 <= s1len; ix1++) {
  //       char ch1 = s1.charAt(ix1 - 1);
  //       double cost = ch1 == ch2 ? 0 : weight.substitute(ix1, ch1, ch2);

  //       double left = column[ix1] + weight.delete(ix1, ch1);
  //       double aboveleft = column[ix1 - 1] + cost;
  //       double above2 = above + weight.insert(ix1, ch2);
  //       double value = Math.min(Math.min(above2, left), aboveleft);
  //       column[ix1 - 1] = above; // write previous
  //       above = value;           // keep previous
  //       smallest = Math.min(smallest, value);
  //     }
  //     column[s1len] = above;

  //     // check if we can stop because we'll be going over the max distance
  //     if (smallest > maxdist)
  //       return smallest;
  //   }

  //   // ok, we're done
  //   return above;
  // }

  private static double estimateCharacters(String s, WeightEstimator e) {
    double sum = 0.0;
    for (int ix = 0; ix < s.length(); ix++)
      sum += Math.min(e.insert(ix, s.charAt(ix)), e.delete(ix, s.charAt(ix)));
    return sum;
  }

  /**
   * The object which supplies the actual weights for editing
   * operations.
   */
  public interface WeightEstimator {

    public double substitute(int pos, char ch1, char ch2);

    public double delete(int pos, char ch);

    public double insert(int pos, char ch);

  }

  public static class DefaultWeightEstimator implements WeightEstimator {
    private double[] charweight; // character number to weight mapping
    private double digits;
    private double letters;
    private double punctuation;
    private double other;

    public DefaultWeightEstimator() {
      this.digits = 2.0;
      this.letters = 1.0;
      this.punctuation = 0.1;
      this.other = 1.0;
      recompute();
    }

    public double substitute(int pos, char ch1, char ch2) {
      return Math.max(insert(pos, ch1), insert(pos, ch2));
    }

    public double delete(int pos, char ch) {
      return insert(pos, ch);
    }

    public double insert(int pos, char ch) {
      if (ch > charweight.length)
        return other;
      return charweight[(int) ch];
    }

    public void setDigitWeight(double digits) {
      this.digits = digits;
      recompute();
    }

    public double getDigitWeight() {
      return digits;
    }

    public void setLetterWeight(double letters) {
      this.letters = letters;
      recompute();
    }

    public void setOtherWeight(double other) {
      this.other = other;
      recompute();
    }

    public void setPunctuationWeight(double punctuation) {
      this.punctuation = punctuation;
      recompute();
    }

    private void recompute() {
      charweight = new double[0xFF];
      for (int ix = 0; ix < charweight.length; ix++) {
        char ch = (char) ix;
        double weight = other;

        if (Character.isLetter(ch))
          weight = letters;
        else if (Character.isDigit(ch))
          weight = digits;
        else {
          int type = Character.getType(ch);
          // 20, 21, 22, 23, 24, 25, 26, 27
          if (Character.isSpace(ch) ||
              (type >= 20 && type <= 27))
            weight = punctuation;
        }

        charweight[ix] = weight;
      }
    }
  }

  // /**
  //  * Utility function for testing Levenshtein performance.
  //  */
  // public static void timing(String s1, String s2) {
  //   final int TIMES = 100000;

  //   System.out.println("----- (" + s1 + ", " + s2 + ")");
  //   WeightEstimator e = new DefaultWeightEstimator();

  //   long time = System.currentTimeMillis();
  //   for (int ix = 0; ix < TIMES; ix++)
  //     distance(s1, s2, e);
  //   System.out.println("default: " + (System.currentTimeMillis() - time));

  //   time = System.currentTimeMillis();
  //   for (int ix = 0; ix < TIMES; ix++)
  //     compactDistance(s1, s2, e);
  //   System.out.println("compact: " + (System.currentTimeMillis() - time));
  // }
}
