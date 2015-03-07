
package no.priv.garshol.duke.databases;

import java.io.IOException;
import java.util.Collection;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.utils.TestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

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

    Collection<Record> recs = db.findCandidateMatches(record);
    assertEquals("wrong number of records found", 1, recs.size());
    r = recs.iterator().next();
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

  @Test
  public void testChangeRecord() throws IOException {
    // index a record, then close the db. reopen, then index a changed
    // version of the record. now the old version should no longer be
    // available.
    assertTrue("database claims to be in-memory", !db.isInMemory());

    Record record = TestUtils.makeRecord("ID", "1", "NAME", "AND", "EMAIL", "BBBBB");
    db.index(record);
    db.commit();
    db.close();

    db = createDatabase(config);

    // same id, different values
    Record record2 = TestUtils.makeRecord("ID", "1", "NAME", "LARS", "EMAIL", "BARS");
    db.index(record2);
    db.commit();

    Record r = db.findRecordById("1");
    assertTrue("record not found", r != null);
    assertEquals("wrong ID", "1", r.getValue("ID"));
    assertEquals("wrong NAME", "LARS", r.getValue("NAME"));
    assertEquals("wrong EMAIL", "BARS", r.getValue("EMAIL"));

    Collection<Record> recs = db.findCandidateMatches(record2);
    assertEquals("wrong number of records found", 1, recs.size());
    r = recs.iterator().next();
    assertEquals("wrong ID", "1", r.getValue("ID"));
    assertEquals("wrong NAME", "LARS", r.getValue("NAME"));
    assertEquals("wrong EMAIL", "BARS", r.getValue("EMAIL"));

    recs = db.findCandidateMatches(record);
    assertEquals("wrong number of records found", 0, recs.size());
  }
}