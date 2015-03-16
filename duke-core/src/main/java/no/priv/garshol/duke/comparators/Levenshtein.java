
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;

/**
 * An implementation of the Levenshtein distance metric. This is a
 * fairly complicated metric, and there are a number of different ways
 * to implement it, with different performance characteristics. As
 * this comparator is highly performance-critical, this class contains
 * a number of different implementations, some of them experimental.
 *
 * <p>Some <a href="http://www.let.rug.nl/kleiweg/lev/">general
 * background on Levenshtein</a>, and <a
 * href="http://stackoverflow.com/questions/4057513/levenshtein-distance-algorithm-better-than-onm">a StackOverflow page on faster algorithms.
 *
 * <p>To see which algorithms are implemented, see comments on
 * individual methods.
 */
public class Levenshtein implements Comparator {

  public double compare(String s1, String s2) {   
    int len = Math.min(s1.length(), s2.length());

    // we know that if the outcome here is 0.5 or lower, then the
    // property will return the lower probability. so the moment we
    // learn that probability is 0.5 or lower we can return 0.0 and
    // stop. this optimization makes a perceptible improvement in
    // overall performance.
    int maxlen = Math.max(s1.length(), s2.length());
    if ((double) len / (double) maxlen <= 0.5)
      return 0.0;

    // if the strings are equal we can stop right here.
    if (len == maxlen && s1.equals(s2))
      return 1.0;
    
    // we couldn't shortcut, so now we go ahead and compute the full
    // metric
    int dist = Math.min(compactDistance(s1, s2), len);
    return 1.0 - (((double) dist) / ((double) len));
  }

  public boolean isTokenized() {
    return true;
  }

  /**
   * This is the original, naive implementation, using the Wagner &
   * Fischer algorithm from 1974. It uses a flattened matrix for
   * speed, but still computes the entire matrix.
   */
  public static int distance(String s1, String s2) {
    if (s1.length() == 0)
      return s2.length();
    if (s2.length() == 0)
      return s1.length();

    int s1len = s1.length();
    // we use a flat array for better performance. we address it by
    // s1ix + s1len * s2ix. this modification improves performance
    // by about 30%, which is definitely worth the extra complexity.
    int[] matrix = new int[(s1len + 1) * (s2.length() + 1)];
    for (int col = 0; col <= s2.length(); col++)
      matrix[col * s1len] = col;
    for (int row = 0; row <= s1len; row++)
      matrix[row] = row;

    for (int ix1 = 0; ix1 < s1len; ix1++) {
      char ch1 = s1.charAt(ix1);
      for (int ix2 = 0; ix2 < s2.length(); ix2++) {
        int cost;
        if (ch1 == s2.charAt(ix2))
          cost = 0;
        else
          cost = 1;

        int left = matrix[ix1 + ((ix2 + 1) * s1len)] + 1;
        int above = matrix[ix1 + 1 + (ix2 * s1len)] + 1;
        int aboveleft = matrix[ix1 + (ix2 * s1len)] + cost;
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
  //  * An optimized version of the Wagner & Fischer algorithm, which
  //  * exploits our knowledge that if the distance is above a certain
  //  * limit (0.5 when normalized) we use the lower probability. We
  //  * therefore stop once we go over the maximum distance.
  //  *
  //  * <p>On at least one use case, this optimization shaves 15% off the
  //  * total execution time (ie: not just Levenshtein).
  //  */
  // public static int cutoffDistance(String s1, String s2) {
  //   if (s1.length() == 0)
  //     return s2.length();
  //   if (s2.length() == 0)
  //     return s1.length();

  //   int maxdist = Math.min(s1.length(), s2.length()) / 2;

  //   int s1len = s1.length();
  //   // we use a flat array for better performance. we address it by
  //   // s1ix + s1len * s2ix. this modification improves performance
  //   // by about 30%, which is definitely worth the extra complexity.
  //   int[] matrix = new int[(s1len + 1) * (s2.length() + 1)];
  //   for (int col = 0; col <= s2.length(); col++)
  //     matrix[col * s1len] = col;
  //   for (int row = 0; row <= s1len; row++)
  //     matrix[row] = row;

  //   for (int ix1 = 0; ix1 < s1len; ix1++) {
  //     char ch1 = s1.charAt(ix1);
  //     for (int ix2 = 0; ix2 < s2.length(); ix2++) {
  //       int cost;
  //       if (ch1 == s2.charAt(ix2))
  //         cost = 0;
  //       else
  //         cost = 1;

  //       int left = matrix[ix1 + ((ix2 + 1) * s1len)] + 1;
  //       int above = matrix[ix1 + 1 + (ix2 * s1len)] + 1;
  //       int aboveleft = matrix[ix1 + (ix2 * s1len)] + cost;
  //       int distance = Math.min(left, Math.min(above, aboveleft));
  //       if (ix1 == ix2 && distance > maxdist)
  //          return distance;
  //       matrix[ix1 + 1 + ((ix2 + 1) * s1len)] = distance;
  //     }
  //   }
    
  //   return matrix[s1len + (s2.length() * s1len)];
  // }

  // /**
  //  * This implementation is my own reinvention of Ukkonen's optimized
  //  * version of the Wagner & Fischer algorithm. It's not exactly the
  //  * same as Ukkonen's algorithm, and I only managed to formulate it
  //  * recursively. The result is that unless s1 and s2 are very similar
  //  * it is slower than Wagner & Fischer. I don't recommend using this
  //  * version.
  //  */
  // public static int recursiveDistance(String s1, String s2) {
  //   if (s1.length() == 0)
  //     return s2.length();
  //   if (s2.length() == 0)
  //     return s1.length();

  //   // we use a flat array for better performance. we address it by
  //   // s1ix + s1len * s2ix. this modification improves performance
  //   // by about 30%, which is definitely worth the extra complexity.
  //   int[] matrix = new int[(s1.length() + 1) * (s2.length() + 1)];
  //   // FIXME: modify to avoid having to initialize
  //   for (int ix = 1; ix < matrix.length; ix++)
  //     matrix[ix] = -1;
    
  //   return computeRecursively(matrix, s1, s2, s1.length(), s2.length());
  // }

  // // inner recursive function for above method
  // private static int computeRecursively(int[] matrix, String s1, String s2,
  //                                       int ix1, int ix2) {
  //   // for the first row and first column we know the score already
  //   if (ix1 == 0)
  //     return ix2;
  //   if (ix2 == 0)
  //     return ix1;

  //   // work out our position in the matrix, and see if we know the score
  //   int pos = ix1 + (ix2 * s1.length());
  //   if (matrix[pos] != -1)
  //     return matrix[pos];

  //   // the lowest possible score in this position
  //   int lowest = Math.abs(ix1 - ix2);

  //   // increase estimate based on lowest score at diagonal
  //   int smallest = Math.min(ix1, ix2);
  //   int cost_smallest = matrix[smallest + (smallest * s1.length())];
  //   if (cost_smallest != -1)
  //     lowest += cost_smallest;

  //   // find the cost here
  //   int cost;
  //   if (s1.charAt(ix1 - 1) == s2.charAt(ix2 - 1))
  //     cost = 0;
  //   else
  //     cost = 1;

  //   // if aboveleft is already at the lowest, we're done
  //   int aboveleft = computeRecursively(matrix, s1, s2, ix1 - 1, ix2 - 1);
  //   if (aboveleft == lowest) {
  //     matrix[pos] = lowest + cost;
  //     return lowest + cost;
  //   }

  //   // what about above?
  //   int above = computeRecursively(matrix, s1, s2, ix1, ix2 - 1);
  //   int left;
  //   if (above > lowest)
  //     // could be lower than above, so compute
  //     left = computeRecursively(matrix, s1, s2, ix1 - 1, ix2);
  //   else
  //     // it' can't be smaller than above, so no need to compute
  //     left = above;
    
  //   int distance = Math.min(left, Math.min(above, aboveleft)) + cost;
  //   matrix[pos] = distance;
  //   return distance;
  // }

  /**
   * Optimized version of the Wagner & Fischer algorithm that only
   * keeps a single column in the matrix in memory at a time. It
   * implements the simple cutoff, but otherwise computes the entire
   * matrix. It is roughly twice as fast as the original function.
   */
  public static int compactDistance(String s1, String s2) {
    if (s1.length() == 0)
      return s2.length();
    if (s2.length() == 0)
      return s1.length();

    // the maximum edit distance there is any point in reporting.
    int maxdist = Math.min(s1.length(), s2.length()) / 2;
    
    // we allocate just one column instead of the entire matrix, in
    // order to save space.  this also enables us to implement the
    // algorithm somewhat faster.  the first cell is always the
    // virtual first row.
    int s1len = s1.length();
    int[] column = new int[s1len + 1];

    // first we need to fill in the initial column. we use a separate
    // loop for this, because in this case our basis for comparison is
    // not the previous column, but a virtual first column.
    int ix2 = 0;
    char ch2 = s2.charAt(ix2);
    column[0] = 1; // virtual first row
    for (int ix1 = 1; ix1 <= s1len; ix1++) {
      int cost = s1.charAt(ix1 - 1) == ch2 ? 0 : 1;

      // Lowest of three: above (column[ix1 - 1]), aboveleft: ix1 - 1,
      // left: ix1. Latter cannot possibly be lowest, so is
      // ignored.
      column[ix1] = Math.min(column[ix1 - 1], ix1 - 1) + cost;
    }

    // okay, now we have an initialized first column, and we can
    // compute the rest of the matrix.
    int above = 0;
    for (ix2 = 1; ix2 < s2.length(); ix2++) {
      ch2 = s2.charAt(ix2);
      above = ix2 + 1; // virtual first row

      int smallest = s1len * 2; // used to implement cutoff
      for (int ix1 = 1; ix1 <= s1len; ix1++) {
        int cost = s1.charAt(ix1 - 1) == ch2 ? 0 : 1;

        // above:     above
        // aboveleft: column[ix1 - 1]
        // left:      column[ix1]
        int value = Math.min(Math.min(above, column[ix1 - 1]), column[ix1]) +
                    cost;
        column[ix1 - 1] = above; // write previous
        above = value;           // keep current
        smallest = Math.min(smallest, value);
      }
      column[s1len] = above;

      // check if we can stop because we'll be going over the max distance
      if (smallest > maxdist)
        return smallest;
    }

    // ok, we're done
    return above;
  }  
}