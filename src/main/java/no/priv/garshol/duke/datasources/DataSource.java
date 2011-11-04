
package no.priv.garshol.duke.datasources;

import no.priv.garshol.duke.Logger;
import no.priv.garshol.duke.RecordIterator;


/**
 * Any class which implements this interface can be used as a data
 * source, so you can plug in your own data sources. Configuration
 * properties are received as bean setter calls via reflection.
 */
public interface DataSource {

  /**
   * Return an iterator over all the records in this data source. This
   * should preferably not load all records into memory, but instead
   * produce them lazily.
   */
  public RecordIterator getRecords();

  /**
   * Gives the data source a logger to report diagnostic information
   * to. Ignoring the logger is allowed.</p>
   *
   * <p><b>WARN:</b> This method is experimental. I'm far from certain
   * that this is how I want this to work. May go for slf4j logging
   * instead, or something similar.
   */
  public void setLogger(Logger logger);
}