
package no.priv.garshol.duke;

/**
 * Extended Record interface with support for modification. Mainly
 * used by RecordBuilder.
 * @since 1.2
 */
public interface ModifiableRecord extends Record {

  /**
   * Adds a new value to the record.
   */
  public void addValue(String property, String value);

  /**
   * Returns true iff the record has no values.
   */
  public boolean isEmpty();
}