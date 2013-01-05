
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
    private double digits;
    private double letters;
    private double punctuation;
    private double other;

    public DefaultWeightEstimator() {
      this.digits = 2.0;
      this.letters = 1.0;
      this.punctuation = 0.1;
      this.other = 1.0;
    }
    
    public double substitute(int pos, char ch1, char ch2) {
      return Math.max(insert(pos, ch1), insert(pos, ch2));
    }
    
    public double delete(int pos, char ch) {
      return insert(pos, ch);
    }

    public double insert(int pos, char ch) {
      if ((ch >= 'a' && ch <= 'z') ||
          (ch >= 'A' && ch <= 'Z'))
        return letters;
      else if (ch >= '0' && ch <= '9')
        return digits;
      else if (ch == ' ' || ch == '\'' || ch == ',' || ch == '-' || ch == '/' ||
               ch == '\\' || ch == '.' || ch == ';' || ch == '(' || ch == ')')
        return punctuation;
      else
        return other;
    }

    public void setDigitWeight(double digits) {
      this.digits = digits;
    }

    public void setLetterWeight(double letters) {
      this.letters = letters;
    }

    public void setOtherWeight(double other) {
      this.other = other;
    }

    public void setPunctuationWeight(double punctuation) {
      this.punctuation = punctuation;
    }
  }
}