
package no.priv.garshol.duke.databases;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Database;

public class LuceneDatabaseTest extends DatabaseTest {

  public Database createDatabase(Configuration config) {
    Database db = new LuceneDatabase();
    db.setOverwrite(true);
    db.setConfiguration(config);
    return db;
  }
  
}