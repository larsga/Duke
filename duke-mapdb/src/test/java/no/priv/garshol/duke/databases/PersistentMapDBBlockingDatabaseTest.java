
package no.priv.garshol.duke.databases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Record;

public class PersistentMapDBBlockingDatabaseTest extends PersistentDatabaseTest {
  private String dbfile;

  public Database createDatabase(Configuration config) throws IOException {
    if (dbfile == null)
      dbfile = tmpdir.newFile().getAbsolutePath(); // ensure same every time

    MapDBBlockingDatabase db = new MapDBBlockingDatabase();
    db.setConfiguration(config);
    db.setOverwrite(false);
    db.setFile(dbfile);
    db.setAsync(false); // slows down tests too much
    db.setWindowSize(0); // otherwise we'll find way too many candidates
  
    Collection<KeyFunction> functions = new ArrayList();
    functions.add(new TestKeyFunction());
    db.setKeyFunctions(functions);
    return db;
  }

  private static class TestKeyFunction implements KeyFunction {
    public String makeKey(Record record) {
      return record.getValue("NAME");
    }
  }
}