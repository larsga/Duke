
package no.priv.garshol.duke.databases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.comparators.ExactComparator;
import no.priv.garshol.duke.utils.TestUtils;
import org.apache.lucene.index.CorruptIndexException;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class DocumentRecordTest {
  private Database db;
  
  @Before
  public void setup() throws CorruptIndexException, IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    props.add(new PropertyImpl("NAME", comp, 0.3, 0.8));
    ConfigurationImpl config = new ConfigurationImpl();
    config.setProperties(props);
    config.setThreshold(0.45);
    db = new LuceneDatabase();
    db.setConfiguration(config);
  }

  @Test
  public void testNormal() throws IOException {
    // First, index up the record
    Record r = TestUtils.makeRecord("ID", "abc", "NAME", "b");
    db.index(r);
    db.commit();

    // Then, retrieve it and verify that it's correct
    r = db.findRecordById("abc");
    assertEquals("abc", r.getValue("ID"));
    assertEquals("b", r.getValue("NAME"));
  }

  @Test
  public void testMultiValue() throws IOException {
    // First, index up the record
    HashMap props = new HashMap();
    props.put("ID", Collections.singleton("abc"));
    Collection<String> list = new ArrayList();
    list.add("b");
    list.add("c");
    props.put("NAME", list);
    Record r = new RecordImpl(props);
    db.index(r);
    db.commit();

    // Then, retrieve it and verify that it's correct
    r = db.findRecordById("abc");
    assertEquals("abc", r.getValue("ID"));
    list = r.getValues("NAME");
    assertEquals(2, list.size());
    assertTrue(list.contains("b"));
    assertTrue(list.contains("c"));
  }

  @Test
  public void testNonExistentField() throws IOException {
    // First, index up the record
    Record r = TestUtils.makeRecord("ID", "abc", "NAME", "b");
    db.index(r);
    db.commit();

    // Then, retrieve it and verify that it's correct
    r = db.findRecordById("abc");
    assertEquals(null, r.getValue("DIDGERIDOO"));
    assertEquals(0, r.getValues("DIDGERIDOO").size());
  }

  @Test
  public void testEquality() throws IOException {
    // First, index up the record
    Record r = TestUtils.makeRecord("ID", "abc", "NAME", "b");
    db.index(r);
    db.commit();

    // Then, retrieve it and verify that it's correct
    r = db.findRecordById("abc");
    Record r2 = db.findRecordById("abc");

    assertTrue(r.hashCode() == r2.hashCode());
    assertTrue(r.equals(r2));
  }
  
}