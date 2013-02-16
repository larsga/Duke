
package no.priv.garshol.duke;

import java.util.Iterator;
import java.util.Collection;

// FIXME: if we are going to implement retraction we will need
// something like a linkdatabase as backing. probably equiv dbs need
// to be aware of the linkdatabase anyway, in order to avoid known bad
// links and make use of extra known links not inferred from data etc.

/**
 * A tool for collecting matching records into groups where all
 * records are considered to match. Note that this means treating the
 * matching relation between records as transitive, which in practice
 * it is not.
 */
public interface EquivalenceClassDatabase {

  /**
   * Returns the number of equivalence classes in the database.
   */
  public int getClassCount();

  /**
   * Returns an iterator over all the classes in the database.
   */
  public Iterator<Collection<String>> getClasses();
  
  /**
   * Get all records linked to the given record (that is, all records
   * in the same equivalence class as the given record).
   * @param id the ID of a record
   * @return Always returns a collection, but it may be empty.
   */
  public Collection<String> getClass(String id);
  
  /**
   * Add a new link between two records.
   */
  public void addLink(String id1, String id2);
  
  /**
   * Commit changes made to persistent store.
   */
  public void commit();
  
}