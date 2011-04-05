
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

  public Database getDatabase() {
    return database;
  }

  protected void setDatabase(Database database) {
    this.database = database;
  }
}