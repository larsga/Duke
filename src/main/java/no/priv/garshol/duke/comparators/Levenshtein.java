
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;

/**
 * An implementation of the Levenshtein distance metric.
 */
public class Levenshtein implements Comparator {

  public double compare(String s1, String s2) {
    int len = Math.min(s1.length(), s2.length());
    int dist = Math.min(distance(s1, s2), len);
    return 1.0 - (((double) dist) / ((double) len));
  }

  public boolean isTokenized() {
    return true;
  }
  
  public static int distance(String s1, String s2) {
    if (s1.length() == 0)
      return s2.length();
    if (s2.length() == 0)
      return s1.length();

    int s1len = s1.length();
    // we use a flat array for better performance. we address it by
    // s1ix + s1len * s2ix. this modification improves performance
    // by about 30%. not sure it's worth the extra code complexity.
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
    
    return matrix[s1len + (s2.length() * s1len)];
  }
  
}