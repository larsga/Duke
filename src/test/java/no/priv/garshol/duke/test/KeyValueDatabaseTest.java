
package no.priv.garshol.duke.test;

import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.KeyValueDatabase;
import no.priv.garshol.duke.DatabaseProperties;

public class KeyValueDatabaseTest extends DatabaseTest {

  public Database createDatabase(Configuration config) {
    Database db = new KeyValueDatabase();
    db.setConfiguration(config);
    db.setDatabaseProperties(new DatabaseProperties()); // sets defaults
    return db;
  }
  
}