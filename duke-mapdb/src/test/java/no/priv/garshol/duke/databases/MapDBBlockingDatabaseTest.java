
package no.priv.garshol.duke.databases;

import java.util.ArrayList;
import java.util.Collection;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Record;

public class MapDBBlockingDatabaseTest extends DatabaseTest {

  public Database createDatabase(Configuration config) {
    MapDBBlockingDatabase db = new MapDBBlockingDatabase();
    db.setConfiguration(config);

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