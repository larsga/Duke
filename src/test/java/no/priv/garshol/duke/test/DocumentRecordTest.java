
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import junit.framework.AssertionFailedError;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.LuceneDatabase;
import no.priv.garshol.duke.comparators.ExactComparator;

public class DocumentRecordTest {
  private Database db;
  
  @Before
  public void setup() throws CorruptIndexException, IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new Property("ID"));
    props.add(new Property("NAME", comp, 0.3, 0.8));
    Configuration config = new Configuration();
    config.setProperties(props);
    config.setThreshold(0.45);
    db = new LuceneDatabase(config, false);
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
  
}