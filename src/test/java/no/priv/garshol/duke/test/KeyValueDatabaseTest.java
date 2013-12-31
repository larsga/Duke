
package no.priv.garshol.duke.test;

import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.KeyValueDatabase;

public class KeyValueDatabaseTest extends DatabaseTest {

  public Database createDatabase(Configuration config) {
    Database db = new KeyValueDatabase();
    db.setConfiguration(config);
    return db;
  }
  
}