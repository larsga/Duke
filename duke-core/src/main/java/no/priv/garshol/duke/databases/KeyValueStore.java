
package no.priv.garshol.duke.databases;

import no.priv.garshol.duke.Record;

/**
 * An interface encapsulating the way KeyValueDatabase interacts with
 * the underlying database. Used to allow different key/value
 * databases to be plugged in and tested.
 */
public interface KeyValueStore {
  
  /**
   * Returns true iff the database is held entirely in memory, and
   * thus is not persistent.
   */
  public boolean isInMemory();

  /**
   * Flushes all changes to disk. For in-memory databases this is a
   * no-op.
   */
  public void commit();
  
  /**
   * Stores state to disk and closes all open resources.
   */
  public void close();

  /**
   * Returns a new internal record ID.
   */
  public long makeNewRecordId();

  /**
   * Stores the entire record under the given internal record ID.
   */
  public void registerRecord(long id, Record record);
  
  /**
   * Records that this external ID refers to the given internal record
   * ID.
   * @param id the internal record ID
   * @param extid the external ID
   */
  public void registerId(long id, String extid);
  
  /**
   * Records that the given token occurred in the given record.
   * @param id the ID of the record the token occurred in
   * @param propname the property the token occurred in
   * @param token the actual token
   */
  public void registerToken(long id, String propname, String token);

  /**
   * Returns the record with the given external ID.
   */
  public Record findRecordById(String extid);

  /**
   * Returns the record with the given internal ID. This method must
   * be thread-safe.
   */
  public Record findRecordById(long id);

  /**
   * Returns the IDs of all records which have the given token in a
   * value for this property. This method must be thread-safe.
   */
  public Bucket lookupToken(String propname, String token);
}
