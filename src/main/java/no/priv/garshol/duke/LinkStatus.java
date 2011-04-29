
package no.priv.garshol.duke;

/**
 * Represents the status of a link between two identities. That is, do
 * we believe it, and why?
 */
public enum LinkStatus {
  /**
   * Means we have outside evidence indicating this is true.
   */
  ASSERTED,

  /**
   * Means Duke has worked this out on its own.
   */
  INFERRED,

  /**
   * Means Duke used to believe this, but have since changed its mind.
   */
  RETRACTED;
}