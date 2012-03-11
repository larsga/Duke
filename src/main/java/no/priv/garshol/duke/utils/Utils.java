
package no.priv.garshol.duke.utils;

public class Utils {

  /**
   * Combines two probabilities using Bayes' theorem.
   */
  public static double computeBayes(double prob1, double prob2) {
    return (prob1 * prob2) /
      ((prob1 * prob2) + ((1.0 - prob1) * (1.0 - prob2)));
  }
  
}