
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
   * Look up potentially matching records. This method must be
   * thread-safe.
   */
  public Collection<Record> findCandidateMatches(Record record);
  
  /**
   * Stores state to disk and closes all open resources.
   */
  public void close();

  /**
   * Gives the database its configuration (called by Duke framework).
   * @since 1.2
   */
  public void setConfiguration(Configuration config);

  /**
   * Sets whether or not to overwrite any existing index (called by
   * Duke framework).
   * @since 1.2
   */
  public void setOverwrite(boolean overwrite);
}
