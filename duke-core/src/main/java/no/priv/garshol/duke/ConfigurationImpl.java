
package no.priv.garshol.duke;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.priv.garshol.duke.utils.Utils;

/**
 * Holds the configuration details for a dataset.
 */
public class ConfigurationImpl implements Configuration {

  // there are two modes: deduplication and record linkage. in
  // deduplication mode all sources are in 'datasources'. in record
  // linkage mode they are in 'group1' and 'group2'. couldn't think
  // of a better solution. sorry.
  private Collection<DataSource> datasources;
  private Collection<DataSource> group1;
  private Collection<DataSource> group2;

  private double threshold;
  private double thresholdMaybe;

  private Map<String, Property> properties;
  private List<Property> proplist; // duplicate to preserve order
  private Collection<Property> lookups; // subset of properties

  private Database database1;
  private Database database2; // used for record linkage, if necessary

  private List<Comparator> customComparators;

  public ConfigurationImpl() {
    this.datasources = new ArrayList();
    this.group1 = new ArrayList();
    this.group2 = new ArrayList();
    this.customComparators = new ArrayList<Comparator>();
  }

  /**
   * Returns the data sources to use (in deduplication mode; don't use
   * this method in record linkage mode).
   */
  public Collection<DataSource> getDataSources() {
    return datasources;
  }

  /**
   * Returns the data sources belonging to a particular group of data
   * sources. Data sources are grouped in record linkage mode, but not
   * in deduplication mode, so only use this method in record linkage
   * mode.
   */
  public Collection<DataSource> getDataSources(int groupno) {
    if (groupno == 1)
      return group1;
    else if (groupno == 2)
      return group2;
    else
      throw new DukeConfigException("Invalid group number: " + groupno);
  }

  /**
   * Adds a data source to the configuration. If in deduplication mode
   * groupno == 0, otherwise it gives the number of the group to which
   * the data source belongs.
   */
  public void addDataSource(int groupno, DataSource datasource) {
    // the loader takes care of validation
    if (groupno == 0)
      datasources.add(datasource);
    else if (groupno == 1)
      group1.add(datasource);
    else if (groupno == 2)
      group2.add(datasource);
  }

  public Database getDatabase(boolean overwrite) {
    return getDatabase(1, overwrite);
  }

  public Database getDatabase(int groupno, boolean overwrite) {
    Database thedb;
    if (groupno == 1) {
      if (database1 == null) // not set, so use default with is in memory
        database1 = new no.priv.garshol.duke.databases.InMemoryDatabase();
      thedb = database1;
    } else if (groupno == 2)
      thedb = database2; // no default for no 2
    else
      throw new DukeException("Can only have two databases");

    if (thedb != null) {
      thedb.setConfiguration(this);
      thedb.setOverwrite(overwrite); // hmmm?
    }
    return thedb;
  }

  public void addDatabase(Database database) {
    if (database1 == null)
      database1 = database;
    else if (database2 == null)
      database2 = database;
    else
      throw new DukeConfigException("Too many database objects configured");
  }

  /**
   * The probability threshold used to decide whether two records
   * represent the same entity. If the probability is higher than this
   * value, the two records are considered to represent the same
   * entity.
   */
  public double getThreshold() {
    return threshold;
  }

  /**
   * Sets the probability threshold for considering two records
   * equivalent.
   */
  public void setThreshold(double threshold) {
    this.threshold = threshold;
  }

  /**
   * The probability threshold used to decide whether two records may
   * represent the same entity. If the probability is higher than this
   * value, the two records are considered possible matches. Can be 0,
   * in which case no records are considered possible matches.
   */
  public double getMaybeThreshold() {
    return thresholdMaybe;
  }

  /**
   * Returns true iff we are in deduplication mode.
   */
  public boolean isDeduplicationMode() {
    return !getDataSources().isEmpty();
  }

  /**
   * Sets the probability threshold for considering two records
   * possibly equivalent. Does not have to be set.
   */
  public void setMaybeThreshold(double thresholdMaybe) {
    this.thresholdMaybe = thresholdMaybe;
  }

  /**
   * The set of properties Duke is to work with.
   */
  public void setProperties(List<Property> props) {
    this.proplist = props;
    this.properties = new HashMap(props.size());
    for (Property prop : props)
      properties.put(prop.getName(), prop);

    // analyze properties to find lookup set
    findLookupProperties();
  }

  /**
   * The set of properties Duke records can have, and their associated
   * cleaners, comparators, and probabilities.
   */
  public List<Property> getProperties() {
    return proplist;
  }

  /**
   * The properties which are used to identify records, rather than
   * compare them.
   */
  public Collection<Property> getIdentityProperties() {
    Collection<Property> ids = new ArrayList();
    for (Property p : getProperties())
      if (p.isIdProperty())
        ids.add(p);
    return ids;
  }

  /**
   * Returns the property with the given name, or null if there is no
   * such property.
   */
  public Property getPropertyByName(String name) {
    return properties.get(name);
  }

  /**
   * Returns the properties Duke queries for in the Lucene index. This
   * is a subset of getProperties(), and is computed based on the
   * probabilities and the threshold.
   */
  public Collection<Property> getLookupProperties() {
    return lookups;
  }

  /**
   * Validates the configuration to verify that it makes sense.
   * Rejects configurations that will fail during runtime.
   */
  public void validate() {
    // verify that we do have properties
    if (properties == null || properties.isEmpty())
      throw new DukeConfigException("Configuration has no properties at all");

    // check if max prob is below threshold
    // this code duplicates code in findLookupProperties(), but prefer
    // that to creating an attribute
    double prob = 0.5;
    for (Property prop : properties.values()) {
      if (prop.getHighProbability() == 0.0)
        // if the probability is zero we ignore the property entirely
        continue;

      prob = Utils.computeBayes(prob, prop.getHighProbability());
    }
    if (prob < threshold)
      throw new DukeConfigException("Maximum possible probability is " + prob +
                                 ", which is below threshold (" + threshold +
                                 "), which means no duplicates will ever " +
                                 "be found");

    // check that we have at least one ID property
    if (getIdentityProperties().isEmpty())
      throw new DukeConfigException("No ID properties.");

    // check that we only have one ID property
    if (getIdentityProperties().size() > 1)
      throw new DukeConfigException("Can only have one ID property.");
  }

  private void findLookupProperties() {
    List<Property> candidates = new ArrayList();
    for (Property prop : properties.values())
      // leave out properties that are either not used for comparisons,
      // or which have lookup turned off explicitly
      if (!prop.isIdProperty() &&
          !prop.isIgnoreProperty() &&
          prop.getLookupBehaviour() != Property.Lookup.FALSE &&
          prop.getHighProbability() != 0.0)
        candidates.add(prop);


    // sort them, lowest high prob to highest high prob
    Collections.sort(candidates, new HighComparator());

    // run over and find all those needed to get above the threshold
    int last = -1;
    double prob = 0.5;
    for (int ix = 0; ix < candidates.size(); ix++) {
      Property prop = candidates.get(ix);
      prob = Utils.computeBayes(prob, prop.getHighProbability());
      if (prob >= threshold) {
        last = ix;
        break;
      }
    }

    if (last == -1)
      lookups = new ArrayList();
    else
      lookups = new ArrayList(candidates.subList(0, last + 1));


    // need to also add TRUE and REQUIRED
    for (Property p : proplist) {
      if (p.getLookupBehaviour() != Property.Lookup.TRUE &&
          p.getLookupBehaviour() != Property.Lookup.REQUIRED)
        continue;

      if (lookups.contains(p))
        continue;

      lookups.add(p);
    }
  }

  private static class HighComparator implements java.util.Comparator<Property> {
    public int compare(Property p1, Property p2) {
      if (p1.getHighProbability() < p2.getHighProbability())
        return 1;
      else if (p1.getHighProbability() == p2.getHighProbability())
        return 0;
      else
        return -1;
    }
  }

  public Configuration copy() {
    ConfigurationImpl copy = new ConfigurationImpl();
    for (DataSource src : datasources)
      copy.addDataSource(0, src);
    for (DataSource src : group1)
      copy.addDataSource(1, src);
    for (DataSource src : group2)
      copy.addDataSource(2, src);

    copy.setThreshold(threshold);
    copy.setMaybeThreshold(thresholdMaybe);
    copy.addDatabase(database1);
    if (database2 != null)
      copy.addDatabase(database2);

    List<Property> newprops = new ArrayList();
    for (Property p : proplist)
      newprops.add(p.copy());
    copy.setProperties(newprops);

    return copy;
  }


  @Override
  public List<Comparator> getCustomComparators() {
	return this.customComparators;
  }

  @Override
  public void addCustomComparator(Comparator comparator) {
	this.customComparators.add(comparator);
  }
}
