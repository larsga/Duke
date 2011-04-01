
package no.priv.garshol.duke;

import java.util.Collection;

/**
 * Represents a record, which may be a single source record from a
 * data source, or a record created from merging data from many
 * records.
 */
public interface Record {

  // we assign these identities? not sure yet. don't think we can, if
  // records are going to match themselves across multiple times we
  // see the same record.  
  public Collection<String> getIdentities();

  public Collection<String> getProperties();
  
  // FIXME: typed values?
  public String getValue(String prop);
 
  public Collection<String> getValues(String prop);
  
  /**
   * Merges the other record into this one.
   */
  public void merge(Record other);

  /**
   * Uses cleaners stored on property definitions in database to clean
   * the data in the record, modifying the record.
   */
  public void clean(Database database);
  
}