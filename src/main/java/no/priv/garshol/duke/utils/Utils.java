
package no.priv.garshol.duke.utils;

public class Utils {

  /**
   * Combines two probabilities using Bayes' theorem.
   */
  public static double computeBayes(double prob1, double prob2) {
    return (prob1 * prob2) /
      ((prob1 * prob2) + ((1.0 - prob1) * (1.0 - prob2)));
  }

  /**
   * Returns true iff we are running on Windows. Used to detect
   * whether it's safe to use Lucene's NIOFSDirectory. It's slow on
   * Windows due to a Java bug.
   */
  public static boolean isWindowsOS() {
    return System.getProperty("os.name").startsWith("Windows");
  }
}