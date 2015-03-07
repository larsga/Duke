
package no.priv.garshol.duke.test;

import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.databases.InMemoryDatabase;

public class InMemoryDatabaseTest extends DatabaseTest {

  public Database createDatabase(Configuration config) {
    Database db = new InMemoryDatabase();
    db.setConfiguration(config);
    return db;
  }
  
}