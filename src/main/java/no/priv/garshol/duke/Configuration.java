
package no.priv.garshol.duke;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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

  public Collection<DataSource> getDataSources() {
    return datasources;
  }

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

  public String getPath() {
    return path;
  }

  // FIXME: means we can create multiple ones. not a good idea.
  public Database createDatabase(boolean overwrite) {
    return new Database(this, overwrite);
  }
  
  public void setPath(String path) {
    this.path = path;
  }

  public double getThreshold() {
    return threshold;
  }

  public void setThreshold(double threshold) {
    this.threshold = threshold;
  }

  public double getMaybeThreshold() {
    return thresholdMaybe;
  }

  public void setMaybeThreshold(double thresholdMaybe) {
    this.thresholdMaybe = thresholdMaybe;
  }

  public void setProperties(List<Property> props) {
    this.proplist = props;
    this.properties = new HashMap(props.size());
    for (Property prop : props)
      properties.put(prop.getName(), prop);

    // analyze properties to find lookup set
    findLookupProperties();
  }

  public List<Property> getProperties() {
    return proplist;
  }

  public Collection<Property> getIdentityProperties() {
    Collection<Property> ids = new ArrayList();
    for (Property p : getProperties())
      if (p.isIdProperty())
        ids.add(p);
    return ids;
  }

  public Property getPropertyByName(String name) {
    return properties.get(name);
  }
  
  public Collection<Property> getLookupProperties() {
    return lookups;
  }  
  
  private void findLookupProperties() {
    List<Property> candidates = new ArrayList();
    for (Property prop : properties.values())
      if (!prop.isIdProperty())
        candidates.add(prop);

    Collections.sort(candidates, new HighComparator());

    int last = -1;
    double prob = 0.5;
    double limit = thresholdMaybe;
    if (limit == 0.0)
      limit = threshold;
    
    for (int ix = 0; ix < candidates.size(); ix++) {
      prob = Utils.computeBayes(prob, candidates.get(ix).getHighProbability());
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

  private class HighComparator implements java.util.Comparator<Property> {
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