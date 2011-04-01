
package no.priv.garshol.duke;

public class Utils {

  public static double computeBayes(double prob1, double prob2) {
    return (prob1 * prob2) /
      ((prob1 * prob2) + ((1.0 - prob1) * (1.0 - prob2)));
  }
  
}