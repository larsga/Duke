
package no.priv.garshol.duke.datasources;

import java.io.IOException;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by damien on 08/04/14.
 */
public class JsonDataSourceTest {
  private JsonDataSource source;

  @Before
  public void setup() {
    source = new JsonDataSource();
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", null, null, null));
    source.addColumn(new Column("F3", null, null, null));
  }

  @Test
  public void testEmpty() throws IOException {
    RecordIterator it = source.getRecordsFromString("");
    assertTrue(!it.hasNext());
  }

  @Test
  public void testSingleRecord() throws IOException {
    Record r = source.getRecordsFromString("{\"F1\":\"a\",\"F2\" : \"b\", \"F3\" : \"c\", \"F4\" : \"d\"}").next();

    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }

  @Test
  public void testArrayField() {
    Record r = source.getRecordsFromString("{\"F1\":[\"a\",\"b\",\"c\"]}").next();
    assertEquals(3, r.getValues("F1").size());
  }

  @Test
  public void testNestRecords() {
    Record r = source.getRecordsFromString("{\"F1\":\"a\",\"FF2\" : {\"F2\" : \"b\"}, \"FFF3\" : {\"FF3\" : {\"F3\" : \"c\",\"F4\" : \"d\"}}}").next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }
  
  @Test
  public void multipleRecords() {
    RecordIterator it = source.getRecordsFromString("{\"F1\":\"a\",\"F2\" : \"b\", \"F3\" : \"c\"}{\"F1\":\"a2\",\"F2\" : \"b2\", \"F3\" : \"c2\"}");
    Record r1 = it.next();
    assertEquals("a", r1.getValue("F1"));
    assertEquals("b", r1.getValue("F2"));
    assertEquals("c", r1.getValue("F3"));
    Record r2 =  it.next();
    assertEquals("a2", r2.getValue("F1"));
    assertEquals("b2", r2.getValue("F2"));
    assertEquals("c2", r2.getValue("F3"));
  }
}

