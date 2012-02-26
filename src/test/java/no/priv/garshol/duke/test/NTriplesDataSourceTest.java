
package no.priv.garshol.duke.test;

import java.io.StringReader;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Column;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.datasources.NTriplesDataSource;

public class NTriplesDataSourceTest {
  private NTriplesDataSource source;
  private static final String RDF_TYPE =
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

  @Before
  public void setup() {
    source = new NTriplesDataSource();
  }
  
  @Test
  public void testEmpty() {
    RecordIterator it = read("");
    assertFalse("empty data source contains records",
                it.hasNext());
  }

  @Test
  public void testEmptyIncremental() {
    source.setIncrementalMode(true);
    testEmpty();
  }

  @Test
  public void testEmptyBlank() {
    RecordIterator it = read("\n\n");
    assertFalse("empty data source contains records",
                it.hasNext());
  }

  @Test
  public void testEmptyBlankInc() {
    source.setIncrementalMode(true);
    testEmptyBlank();
  }

  @Test
  public void testSingleRecord() {
    source.addColumn(new Column("?uri", "ID", null, null));
    source.addColumn(new Column("http://b", "PROP", null, null));
    
    RecordIterator it = read("<http://a> <http://b> \"foo\" .\n");

    Record r = it.next();
    assertEquals("http://a", r.getValue("ID"));
    assertEquals("foo", r.getValue("PROP"));

    assertFalse(it.hasNext());
  }

  @Test
  public void testSingleRecordInc() {
    source.setIncrementalMode(true);
    testSingleRecord();
  }

  @Test
  public void testSingleRecord2() {
    source.addColumn(new Column("?uri", "ID", null, null));
    source.addColumn(new Column("http://b", "PROP", null, null));
    
    RecordIterator it = read("<http://a> <http://b> \"foo\" .\n" +
                             "<http://a> <http://c> \"foo\" .\n");

    Record r = it.next();
    assertEquals("http://a", r.getValue("ID"));
    assertEquals("foo", r.getValue("PROP"));

    assertFalse(it.hasNext());
  }
  
  @Test
  public void testSingleRecord2Inc() {
    source.setIncrementalMode(true);
    testSingleRecord2();
  }

  @Test
  public void testSingleRecord2Spaces() {
    source.addColumn(new Column("?uri", "ID", null, null));
    source.addColumn(new Column("http://b", "PROP", null, null));
    
    RecordIterator it = read("\n<http://a> <http://b> \"foo\" .\n\n" +
                             "<http://a> <http://c> \"foo\" .\n\n");

    Record r = it.next();
    assertEquals("http://a", r.getValue("ID"));
    assertEquals("foo", r.getValue("PROP"));

    assertFalse(it.hasNext());
  }
  
  @Test
  public void testSingleRecord2SpacesInc() {
    source.setIncrementalMode(true);
    testSingleRecord2Spaces();
  }

  @Test
  public void testTwoRecords() {
    source.addColumn(new Column("?uri", "ID", null, null));
    source.addColumn(new Column("http://b", "PROP", null, null));
    
    RecordIterator it = read("<http://a> <http://b> \"foo\" .\n" +
                             "<http://a> <http://c> \"foo\" .\n" +
                             "<http://a> <" + RDF_TYPE + "> \"http://d\" .\n" +
                             "<http://e> <http://b> \"bar\" .\n" +
                             "<http://e> <http://c> \"foo\" .\n" +
                             "<http://e> <" + RDF_TYPE + "> \"http://f\" .\n");

    Record r = it.next();
    checkAorE(r); // we don't know the order
    assertTrue("second record not found", it.hasNext());
    r = it.next();
    checkAorE(r);
  }

  private void checkAorE(Record r) {
    if (r.getValue("ID").equals("http://a"))
      assertEquals("foo", r.getValue("PROP"));
    else {
      assertEquals("http://e", r.getValue("ID"));
      assertEquals("bar", r.getValue("PROP"));
    }
  }

  @Test
  public void testTwoRecordsInc() {
    source.setIncrementalMode(true);
    testTwoRecords();
  }
  
  @Test
  public void testTypeFiltering() {
    source.addColumn(new Column("?uri", "ID", null, null));
    source.addColumn(new Column("http://b", "PROP", null, null));
    source.setAcceptTypes("http://d");
    
    RecordIterator it = read("<http://a> <http://b> \"foo\" .\n" +
                             "<http://a> <http://c> \"foo\" .\n" +
                             "<http://a> <" + RDF_TYPE + "> \"http://d\" .\n" +
                             "<http://e> <http://b> \"bar\" .\n" +
                             "<http://e> <http://c> \"foo\" .\n" +
                             "<http://e> <" + RDF_TYPE + "> \"http://f\" .\n");

    Record r = it.next();
    assertEquals("http://a", r.getValue("ID"));
    assertEquals("foo", r.getValue("PROP"));

    assertFalse("e record not filtered out", it.hasNext());
  }

  @Test
  public void testTypeFilteringInc() {
    source.setIncrementalMode(true);
    testTypeFiltering();
  }
  
  // --- helpers
  
  private RecordIterator read(String csvdata) {
    source.setReader(new StringReader(csvdata));
    return source.getRecords();
  }
}