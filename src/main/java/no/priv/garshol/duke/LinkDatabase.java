
package no.priv.garshol.duke;

import java.util.Collection;

/**
 * A LinkDatabase is a class which can keep track of links between
 * entities.
 */
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
   * Get all links for this identity.
   */
  public Collection<Link> getAllLinksFor(String id);
  
  /**
   * Assert a link.
   */
  public void assertLink(Link link);

  /**
   * Commit asserted links to persistent store.
   */
  public void commit();
}