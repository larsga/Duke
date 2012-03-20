
package no.priv.garshol.duke;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;

import no.priv.garshol.duke.utils.Utils;

/**
 * Holds the configuration details for a dataset.
 */
public class Configuration {

  // there are two modes: deduplication and record linkage. in
  // deduplication mode all sources are in 'datasources'. in record
  // linkage mode they are in 'group1' and 'group2'. couldn't think
  // of a better solution. sorry.
  private Collection<DataSource> datasources;
  private Collection<DataSource> group1;
  private Collection<DataSource> group2;

  private String path;
  private double threshold;
  private double thresholdMaybe;

  private Map<String, Property> properties;
  private List<Property> proplist; // duplicate to preserve order
  private Collection<Property> lookups; // subset of properties
  
  public Configuration() {
    this.datasources = new ArrayList();
    this.group1 = new ArrayList();
    this.group2 = new ArrayList();
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
      throw new RuntimeException("Invalid group number: " + groupno);
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

  /**
   * Returns the path to the Lucene index directory. If null, it means
   * the Lucene index is kept in-memory.
   */
  public String getPath() {
    return path;
  }
  
  /**
   * The path to the Lucene index directory. If null or not set, it
   * means the Lucene index is kept in-memory.
   */
  public void setPath(String path) {
    this.path = path;
  }

  // FIXME: means we can create multiple ones. not a good idea.
  public Database createDatabase(boolean overwrite) {
    return new LuceneDatabase(this, overwrite);
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
  
  private void findLookupProperties() {
    List<Property> candidates = new ArrayList();
    for (Property prop : properties.values())
      if (!prop.isIdProperty() || prop.isIgnoreProperty())
        candidates.add(prop);

    Collections.sort(candidates, new HighComparator());

    int last = -1;
    double prob = 0.5;
    double limit = thresholdMaybe;
    if (limit == 0.0)
      limit = threshold;
    
    for (int ix = 0; ix < candidates.size(); ix++) {
      Property prop = candidates.get(ix);
      if (prop.getHighProbability() == 0.0)
        // if the probability is zero we ignore the property entirely
        continue;

      prob = Utils.computeBayes(prob, prop.getHighProbability());
      if (prob >= threshold) {
        if (last == -1)
          last = ix;
        break;
      }
      if (prob >= limit && last == -1)
        last = ix;
    }

    if (prob < threshold)
      throw new DukeConfigException("Maximum possible probability is " + prob +
                                 ", which is below threshold (" + threshold +
                                 "), which means no duplicates will ever " +
                                 "be found");

    if (last == -1)
      lookups = Collections.EMPTY_LIST;
    else
      lookups = new ArrayList(candidates.subList(last, candidates.size()));
  }

  private static class HighComparator implements java.util.Comparator<Property> {
    public int compare(Property p1, Property p2) {
      if (p1.getHighProbability() < p2.getHighProbability())
        return -1;
      else if (p1.getHighProbability() == p2.getHighProbability())
        return 0;
      else
        return 1;
    }
  }
}