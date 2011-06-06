
package no.priv.garshol.duke;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Holds the configuration details for a dataset.
 */
public class Configuration {
  private Database database;
  // there are two modes: deduplication and record linkage. in
  // deduplication mode all sources are in 'datasources'. in record
  // linkage mode they are in 'group1' and 'group2'. couldn't think
  // of a better solution. sorry.
  private Collection<DataSource> datasources;
  private Collection<DataSource> group1;
  private Collection<DataSource> group2;

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

  /**
   * Returns the record database.
   * @param overwrite whether or not to blank the database on opening
   */
  public Database getDatabase(boolean overwrite) {
    database.openIndexes(overwrite);
    return database;
  }

  protected void setDatabase(Database database) {
    this.database = database;
  }
}