
package no.priv.garshol.duke.databases;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Database;

public class KeyValueDatabaseTest extends DatabaseTest {

  public Database createDatabase(Configuration config) {
    Database db = new KeyValueDatabase();
    db.setConfiguration(config);
    return db;
  }
  
}