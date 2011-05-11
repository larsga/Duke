
package no.priv.garshol.duke;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Holds the configuration details for a dataset.
 */
public class Configuration {
  private Database database;
  private Collection<DataSource> datasources;

  public Configuration() {
    this.datasources = new ArrayList();
  }

  public Collection<DataSource> getDataSources() {
    return datasources;
  }

  public void addDataSource(DataSource datasource) {
    datasources.add(datasource);
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