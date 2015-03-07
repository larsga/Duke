
package no.priv.garshol.duke.databases;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Database;

public class PersistentLuceneDatabaseTest extends PersistentDatabaseTest {

  public Database createDatabase(Configuration config) {
    LuceneDatabase db = new LuceneDatabase();
    db.setOverwrite(false);
    db.setConfiguration(config);
    db.setPath(tmpdir.getRoot().getAbsolutePath());
    return db;
  }
  
}