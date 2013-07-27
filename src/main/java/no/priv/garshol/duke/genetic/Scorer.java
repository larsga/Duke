
package no.priv.garshol.duke.genetic;

/**
 * Computes the score of a record based on how many times it was found
 * as a match.
 */
public interface Scorer {

  public int computeScore(int count);

}
