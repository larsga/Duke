
package no.priv.garshol.duke.genetic;

import no.priv.garshol.duke.LinkKind;

/**
 * An oracle can say whether a given match is correct or not.
 */
public interface Oracle {

  /**
   * Asks the oracle whether the two IDs represent the same thing or
   * not, and returns the answer. MAYBESAME means we don't know.
   */
  public LinkKind getLinkKind(String id1, String id2);
  
}
