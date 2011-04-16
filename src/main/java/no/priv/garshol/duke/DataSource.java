
package no.priv.garshol.duke;

/**
 * Any class which implements this interface can be used as a data
 * source, so you can plug in your own data sources. Configuration
 * properties are received as bean setter calls via reflection.
 */
public interface DataSource {

  public RecordIterator getRecords();
  
}