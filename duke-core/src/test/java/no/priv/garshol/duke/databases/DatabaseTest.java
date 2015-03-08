
package no.priv.garshol.duke.databases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.comparators.ExactComparator;
import no.priv.garshol.duke.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public abstract class DatabaseTest {
  protected Database db;
  protected ConfigurationImpl config;

  @Before
  public void setup() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    props.add(new PropertyImpl("NAME", comp, 0.3, 0.8));
    props.add(new PropertyImpl("EMAIL", comp, 0.3, 0.8));

    config = new ConfigurationImpl();
    config.setProperties(props);
    config.setThreshold(0.85);
    config.setMaybeThreshold(0.8);
    db = createDatabase(config);
  }

  // overridden to create specific databases
  public abstract Database createDatabase(Configuration config)
    throws IOException;

  @Test
  public void testTrivial() throws IOException {
    Record record = TestUtils.makeRecord("ID", "1", "NAME", "AND", "EMAIL", "BBBBB");
    db.index(record);
    db.commit();

    record = db.findRecordById("1");
    assertTrue("no record found", record != null);
    assertEquals("wrong ID", "1", record.getValue("ID"));
    assertEquals("wrong EMAIL", "BBBBB", record.getValue("EMAIL"));
  }

  @Test
  public void testBackslash() throws IOException {
    String name = "\"Lastname, Firstname \\(external\\)\"";
    Record record = TestUtils.makeRecord("ID", "1",
                                         "NAME", name, "EMAIL", "BBBBB");
    db.index(record);
    db.commit();

    Record record2 = TestUtils.makeRecord("NAME", "\"lastname, firstname \\(external\\)\"");
    db.findCandidateMatches(record2);
  }

  @Test
  public void testBNode() throws IOException {
    Record record = TestUtils.makeRecord("ID", "_:RHUKdfPM299", "NAME", "AND", "EMAIL", "BBBBB");
    db.index(record);
    db.commit();

    record = db.findRecordById("_:RHUKdfPM299");
    assertTrue("no record found", record != null);
    assertEquals("wrong ID", "_:RHUKdfPM299", record.getValue("ID"));
  }

  @Test
  public void testURI() throws IOException {
    Record record = TestUtils.makeRecord("ID", "http://norman.walsh.name/knows/who/robin-berjon", "NAME", "AND", "EMAIL", "BBBBB");
    db.index(record);
    db.commit();

    record = db.findRecordById("http://norman.walsh.name/knows/who/robin-berjon");
    assertTrue("no record found", record != null);
    assertEquals("wrong ID", "http://norman.walsh.name/knows/who/robin-berjon",
                 record.getValue("ID"));
  }

  @Test
  public void testTrivialFind() throws IOException {
    Record record = TestUtils.makeRecord("ID", "1", "NAME", "AND", "EMAIL", "BBBBB");
    db.index(record);
    db.commit();

    Collection<Record> cands = db.findCandidateMatches(record);
    assertEquals("no record found", 1, cands.size());
    assertEquals("wrong ID", "1", cands.iterator().next().getValue("ID"));
  }

  @Test
  public void testRecordImplementation() throws IOException {
    Record record = TestUtils.makeRecord("ID", "1", "NAME", "AND", "EMAIL", "BBBBB");
    db.index(record);
    db.commit();

    record = db.findRecordById("1");
    assertEquals("wrong ID", "1", record.getValue("ID"));
    assertEquals("wrong NAME", "AND", record.getValue("NAME"));
    assertEquals("wrong EMAIL", "BBBBB", record.getValue("EMAIL"));

    Collection<String> props = record.getProperties();
    assertEquals("wrong number of properties", 3, props.size());
    assertTrue("no ID", props.contains("ID"));
    assertTrue("no NAME", props.contains("NAME"));
    assertTrue("no EMAIL", props.contains("EMAIL"));
  }

  @Test
  public void testBoostAt1() throws IOException {
    // make own config
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    props.add(new PropertyImpl("NAME", comp, 0.3, 1.0)); // 1.0 !!!
    props.add(new PropertyImpl("EMAIL", comp, 0.3, 0.8));

    config = new ConfigurationImpl();
    config.setProperties(props);
    config.setThreshold(0.85);
    config.setMaybeThreshold(0.8);
    db = createDatabase(config);

    // now we can try
    Record record = TestUtils.makeRecord("ID", "1", "NAME", "George", "EMAIL", "BBBBB");
    db.index(record);
    db.commit();

    Collection<Record> cands = db.findCandidateMatches(record);
    assertEquals("no record found", 1, cands.size());
    assertEquals("wrong ID", "1", cands.iterator().next().getValue("ID"));
  }
}
