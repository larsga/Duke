
package no.priv.garshol.duke.test;

import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.LuceneDatabase;
import no.priv.garshol.duke.DatabaseProperties;

public class LuceneDatabaseTest extends DatabaseTest {

  public Database createDatabase(Configuration config) {
    Database db = new LuceneDatabase();
    db.setOverwrite(true);
    db.setConfiguration(config);
    db.setDatabaseProperties(new DatabaseProperties()); // sets defaults
    return db;
  }
  
}