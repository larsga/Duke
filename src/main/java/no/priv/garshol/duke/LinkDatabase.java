
package no.priv.garshol.duke;

import java.util.Collection;

public interface LinkDatabase {

  /**
   * Returns all links added or retracted since the given time.
   */
  public Collection<Link> getChangesSince(long timestamp);

  /**
   * Get all asserted links.
   */
  public Collection<Link> getAllLinks();

  /**
   * Retracts a previously asserted link.
   */
  public void retractLink(); // FIXME: parameters!

  /**
   * Assert a link.
   */
  public void assertLink(); // FIXME: parameters

  /**
   * Get all links for ID.
   */
  public Collection<Link> getAllLinksFor(String id); // FIXME: ok?
}