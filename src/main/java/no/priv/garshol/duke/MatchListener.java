
package no.priv.garshol.duke;

/**
 * Interface implemented by code which can receive notifications that
 * two records are considered to match.
 */
public interface MatchListener {

  public void matches(Record r1, Record r2, double confidence);
  
}