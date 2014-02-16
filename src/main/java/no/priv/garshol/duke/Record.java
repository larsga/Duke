
package no.priv.garshol.duke;

import java.util.Collection;

/**
 * Represents a record, which may be a single source record from a
 * data source, or a record created from merging data from many
 * records.
 */
public interface Record {

  /**
   * The names of the properties this record has. May be a subset of
   * the properties defined in the configuration if not all properties
   * have values.
   */
  public Collection<String> getProperties();
  
  /**
   * All values for the named property. May be empty. May not contain
   * null or empty strings. Never returns null.
   */
  public Collection<String> getValues(String prop);

  /**
   * Returns a value for the named property. May be null. May not be
   * the empty string. If the property has more than one value there is
   * no way to predict which value is returned.
   */
  public String getValue(String prop);
  
  /**
   * Merges the other record into this one. None of the
   * implementations support this method yet, but it's going to be
   * used when we implement issue 4.
   */
  public void merge(Record other);
  
}
