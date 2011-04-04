
package no.priv.garshol.duke;

import java.util.Collection;

/**
 * Holds the configuration details for a dataset.
 */
public class Configuration {
  private Database database;

  public Collection<DataSource> getDataSources() {
    return null;
  }

  public Database getDatabase() {
    return database;
  }

  protected void setDatabase(Database database) {
    this.database = database;
  }
}