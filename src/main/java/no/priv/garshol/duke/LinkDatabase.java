
package no.priv.garshol.duke;

import java.util.Collection;

public interface LinkDatabase {

  /**
   * Returns all links modified since the given time.
   */
  public Collection<Link> getChangesSince(long since);

  /**
   * Get all links.
   */
  public Collection<Link> getAllLinks();

  /**
   * Assert a link.
   */
  public void assertLink(Link link);

  /**
   * Commit asserted links to persistent store.
   */
  public void commit();
}