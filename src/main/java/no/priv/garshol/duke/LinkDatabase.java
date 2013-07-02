
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
   * Get all links for this identity. If there are no links it returns
   * an empty collection, never null.
   */
  public Collection<Link> getAllLinksFor(String id);
  
  /**
   * Assert a link.
   */
  public void assertLink(Link link);

  /**
   * Can we work out, based on what we know, the relationship between
   * these two? Returns null if we don't know the relationship.
   */
  public Link inferLink(String id1, String id2);

  /**
   * Verifies that we still have a connection to the database, and
   * reestablishes it, if not. Useful when connections live a long
   * time and are rarely used.
   */
  public void validateConnection();
  
  /**
   * Commit asserted links to persistent store.
   */
  public void commit();

  /**
   * Removes all links from the database.
   */
  public void clear();
  
  /**
   * Shuts down the database, releasing resources.
   */
  public void close();
}