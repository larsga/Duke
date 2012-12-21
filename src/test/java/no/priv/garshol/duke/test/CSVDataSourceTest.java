
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.fail;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import junit.framework.AssertionFailedError;

import java.io.IOException;
import java.io.StringReader;

import no.priv.garshol.duke.Column;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.datasources.CSVDataSource;

public class CSVDataSourceTest {
  private CSVDataSource source;
  
  @Before
  public void setup() {
    source = new CSVDataSource();
  }
  
  @Test
  public void testEmpty() throws IOException {
    RecordIterator it = read("");
    assertTrue(!it.hasNext());
  }
  
  @Test
  public void testSingleRecord() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", null, null, null));
    source.addColumn(new Column("F3", null, null, null));
    
    RecordIterator it = read("F1,F2,F3\na,b,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }
  
  @Test
  public void testSingleRecordWithComment() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", null, null, null));
    source.addColumn(new Column("F3", null, null, null));
    source.setSkipLines(1);
    
    RecordIterator it = read("# this is a comment\nF1,F2,F3\na,b,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }
  
  @Test
  public void testSingleRecordWithoutHeader() throws IOException {
    source.addColumn(new Column("1", "F1", null, null));
    source.addColumn(new Column("2", "F2", null, null));
    source.addColumn(new Column("3", "F3", null, null));
    source.setHeaderLine(false);
    
    RecordIterator it = read("a,b,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }
  
  @Test
  public void testSingleRecordWithoutHeaderExtraColumn() throws IOException {
    source.addColumn(new Column("1", "F1", null, null));
    source.addColumn(new Column("2", "F2", null, null));
    source.addColumn(new Column("3", "F3", null, null));
    source.setHeaderLine(false);
    
    RecordIterator it = read("a,b,c,d");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }
  
  @Test
  public void testSingleRecordWithoutHeaderSkipColumn() throws IOException {
    source.addColumn(new Column("1", "F1", null, null));
    source.addColumn(new Column("3", "F3", null, null));
    source.setHeaderLine(false);
    
    RecordIterator it = read("a,b,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals(null, r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }

  @Test
  public void testColumnNotInHeader() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", null, null, null));
    source.addColumn(new Column("F3", null, null, null));
    source.addColumn(new Column("F4", null, null, null));
    
    try {
      RecordIterator it = read("F1,F2,F3\na,b,c");
      Record r = it.next();
      fail("Didn't catch missing column F4");
    } catch (DukeConfigException e) {
      // caught the configuration mistake
    }
  }
  
  private RecordIterator read(String csvdata) {
    source.setReader(new StringReader(csvdata));
    return source.getRecords();
  }
}