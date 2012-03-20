
package no.priv.garshol.duke;

import java.util.Collection;

/**
 * Used to store and index records for later matching.
 */
public interface Database {
  
  /**
   * Returns true iff the database is held entirely in memory, and
   * thus is not persistent.
   */
  public boolean isInMemory();

  /**
   * Add the record to the index.
   */
  public void index(Record record);

  /**
   * Flushes all changes to disk. For in-memory databases this is a
   * no-op.
   */
  public void commit();

  /**
   * Look up record by identity.
   */
  public Record findRecordById(String id);

  /**
   * Look up potentially matching records.
   */
  public Collection<Record> findCandidateMatches(Record record);
  
  /**
   * Stores state to disk and closes all open resources.
   */
  public void close();
}
