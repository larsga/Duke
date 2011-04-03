
package no.priv.garshol.duke;

/**
 * An implementation of the Levenshtein distance metric.
 */
public class Levenshtein {

  public static int distance(String s1, String s2) {
    if (s1.length() == 0)
      return s2.length();
    if (s2.length() == 0)
      return s1.length();

    int[][] matrix = new int[s1.length() + 1][s2.length() + 1];
    for (int col = 0; col <= s2.length(); col++)
      matrix[0][col] = col;
    for (int row = 0; row <= s1.length(); row++)
      matrix[row][0] = row;

    for (int ix1 = 0; ix1 < s1.length(); ix1++) {
      char ch1 = s1.charAt(ix1);
      for (int ix2 = 0; ix2 < s2.length(); ix2++) {
        int cost;
        if (ch1 == s2.charAt(ix2))
          cost = 0;
        else
          cost = 1;

        int left = matrix[ix1][ix2 + 1] + 1;
        int above = matrix[ix1 + 1][ix2] + 1;
        int aboveleft = matrix[ix1][ix2] + cost;
        matrix[ix1 + 1][ix2 + 1] = Math.min(left, Math.min(above, aboveleft));
      }
    }
    
    return matrix[s1.length()][s2.length()];
  }
  
}