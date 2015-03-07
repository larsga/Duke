
package no.priv.garshol.duke;

import java.util.List;
import java.util.Collection;

public interface Configuration {

  /**
   * Returns the data sources to use (in deduplication mode; don't use
   * this method in record linkage mode).
   */
  public Collection<DataSource> getDataSources();

  /**
   * Returns the data sources belonging to a particular group of data
   * sources. Data sources are grouped in record linkage mode, but not
   * in deduplication mode, so only use this method in record linkage
   * mode. The group numbers are 1 and 2.
   */
  public Collection<DataSource> getDataSources(int groupno);

  /**
   * Returns the database to be used for processing.
   * @param overwrite Whether to overwrite existing contents or not.
   * @since 1.2
   */
  public Database getDatabase(boolean overwrite);

  /**
   * Returns the database to be used for a specific group.
   * @param overwrite Whether to overwrite existing contents or not.
   * @since 1.3
   */
  public Database getDatabase(int groupno, boolean overwrite);

  /**
   * The probability threshold used to decide whether two records
   * represent the same entity. If the probability is higher than this
   * value, the two records are considered to represent the same
   * entity.
   */
  public double getThreshold();

  /**
   * The probability threshold used to decide whether two records may
   * represent the same entity. If the probability is higher than this
   * value, the two records are considered possible matches. Can be 0,
   * in which case no records are considered possible matches.
   */
  public double getMaybeThreshold();

  /**
   * Returns true iff we are in deduplication mode.
   */
  public boolean isDeduplicationMode();

  /**
   * The set of properties Duke records can have, and their associated
   * cleaners, comparators, and probabilities.
   */
  public List<Property> getProperties();

  /**
   * The properties which are used to identify records, rather than
   * compare them.
   */
  public Collection<Property> getIdentityProperties();

  /**
   * Returns the property with the given name, or null if there is no
   * such property.
   */
  public Property getPropertyByName(String name);

  /**
   * Returns the properties Duke queries for in the Lucene index. This
   * is a subset of getProperties(), and is computed based on the
   * probabilities and the threshold.
   */
  public Collection<Property> getLookupProperties();

  /**
   * Validates the configuration to verify that it makes sense.
   * Rejects configurations that will fail during runtime.
   */
  public void validate();

  /**
   * Sets the threshold.
   * @since 1.1
   */
  public void setThreshold(double threshold);

  /**
   * Returns an exact copy of the configuration.
   * @since 1.1
   */
  public Configuration copy();

  /**
   * Adds a database object. Used by ConfigLoader. If called only once
   * there is only a single database. Can be called twice.
   * @since 1.3
   */
  public void addDatabase(Database database);

  /**
   * Adds a custom comparator.
   * @since 1.3
   */
  public void addCustomComparator(Comparator comparator);

  /**
   * Returns any customized comparators declared using object tags
   * in the config file.
   * @since 1.3
   */
  public List<Comparator> getCustomComparators();
}
