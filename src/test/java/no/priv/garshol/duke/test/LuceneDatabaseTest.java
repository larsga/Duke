
package no.priv.garshol.duke.test;

import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.LuceneDatabase;

public class LuceneDatabaseTest extends DatabaseTest {

  public Database createDatabase(Configuration config) {
    Database db = new LuceneDatabase();
    db.setOverwrite(true);
    db.setConfiguration(config);
    return db;
  }
  
}