
package no.priv.garshol.duke;

import java.util.Collection;

/**
 * Experimental interface for retrieving link information from outside
 * sources for use inside the Duke processing. Intended to feed into a
 * LinkDatabase.
 */
public interface LinkSource {

  /**
   * Returns the links known by the source.
   */
  public Collection<Link> getLinks();

}