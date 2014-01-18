
package no.priv.garshol.duke.test;

import java.io.IOException;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Database;

/**
 * Adds extra tests for persistent databases.
 */
public abstract class PersistentDatabaseTest extends DatabaseTest {
  @Rule
  public TemporaryFolder tmpdir = new TemporaryFolder();

  @Test
  public void testPersistence() throws IOException {
    // can we index a record, close and reopen the database, and find
    // the same record again afterwards?
    assertTrue("database claims to be in-memory", !db.isInMemory());

    Record record = TestUtils.makeRecord("ID", "1", "NAME", "AND", "EMAIL", "BBBBB");
    db.index(record);
    db.commit();
    db.close();

    db = createDatabase(config);

    Record r = db.findRecordById("1");
    assertTrue("record not found after reopening", r != null);
    assertEquals("wrong ID", "1", r.getValue("ID"));
    assertEquals("wrong NAME", "AND", r.getValue("NAME"));
    assertEquals("wrong EMAIL", "BBBBB", r.getValue("EMAIL"));
  }

  @Test
  public void testOverwrite() throws IOException {
    // can we index a record, close and reopen the database with overwrite
    // set, and not find it again?
    assertTrue("database claims to be in-memory", !db.isInMemory());

    Record record = TestUtils.makeRecord("ID", "1", "NAME", "AND", "EMAIL", "BBBBB");
    db.index(record);
    db.commit();
    db.close();

    db = createDatabase(config);
    db.setOverwrite(true);

    Record r = db.findRecordById("1");
    assertTrue("record found after reopening, despite overwrite", r == null);
  }
}