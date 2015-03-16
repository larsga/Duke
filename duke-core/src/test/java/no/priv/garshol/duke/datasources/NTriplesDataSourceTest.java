
package no.priv.garshol.duke.datasources;

import java.io.StringReader;

import no.priv.garshol.duke.Cleaner;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.cleaners.FamilyCommaGivenCleaner;
import no.priv.garshol.duke.cleaners.RegexpCleaner;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

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

  @Test
  public void testSingleRecordDoubleProp() {
    source.addColumn(new Column("?uri", "ID", null, null));
    source.addColumn(new Column("http://b", "PROP", null, null));
    // yes, we map b two times. might be necessary to split a compound
    // value into two different properties.
    source.addColumn(new Column("http://b", "PROP2", null, null));
    
    RecordIterator it = read("<http://a> <http://b> \"foo\" .\n");

    Record r = it.next();
    assertEquals("http://a", r.getValue("ID"));
    assertEquals("foo", r.getValue("PROP"));
    assertEquals("foo", r.getValue("PROP2"));

    assertFalse(it.hasNext());
  }

  @Test
  public void testSingleRecordDoublePropInc() {
    source.setIncrementalMode(true);
    testSingleRecordDoubleProp();
  }

  @Test
  public void testRealData() {
    String data = "<http://data.deichman.no/person/ahlgren_ernst_1850-1888> <http://data.deichman.no/catalogueName> \"Ahlgren, Ernst\" .\n" +
      "<http://data.deichman.no/person/ahlgren_ernst_1850-1888> <http://data.deichman.no/lifespan> \"1850-1888\" .\n" +
"<http://data.deichman.no/person/ahlgren_ernst_1850-1888> <http://www.foafrealm.org/xfoaf/0.1/nationality> <http://data.deichman.no/nationality/sv> .\n" +
"<http://data.deichman.no/person/ahlgren_ernst_1850-1888> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .\n" +
"<http://data.deichman.no/person/ahlgren_ernst_1850-1888> <http://xmlns.com/foaf/0.1/name> \"Ahlgren, Ernst\" .\n" +
"<http://data.deichman.no/person/ahlgren_ernst_1850-1888> <http://xmlns.com/foaf/0.1/title> \"psevd. for Victoria Benedictsson\" .\n";

    RegexpCleaner birthcleaner = new RegexpCleaner();
    birthcleaner.setRegexp("^(\\d\\d\\d\\d)-");
    RegexpCleaner deathcleaner = new RegexpCleaner();
    deathcleaner.setRegexp("-(\\d\\d\\d\\d)$");
    
    source.addColumn(new Column("?uri", "ID", null, null));    
    source.addColumn(new Column("http://xmlns.com/foaf/0.1/name", "NAME",
                                null, new FamilyCommaGivenCleaner()));
    source.addColumn(new Column("http://data.deichman.no/lifespan",
                                "YEAROFBIRTH", null, birthcleaner));
    source.addColumn(new Column("http://data.deichman.no/lifespan",
                                "YEAROFDEATH", null, deathcleaner));

    RecordIterator it = read(data);
    Record r = it.next();
    assertEquals("http://data.deichman.no/person/ahlgren_ernst_1850-1888",
                 r.getValue("ID"));
    assertEquals("ernst ahlgren", r.getValue("NAME"));
    assertEquals("1850", r.getValue("YEAROFBIRTH"));
    assertEquals("1888", r.getValue("YEAROFDEATH"));

    assertFalse(it.hasNext());
  }

  @Test
  public void testCleanedNullIsDiscarded() {
    source.addColumn(new Column("?uri", "ID", null, null));
    source.addColumn(new Column("http://b", "PROP", null,
                                new NullCleaner()));
    
    RecordIterator it = read("<http://a> <http://b> \"foo\" .\n");

    assertFalse(it.hasNext());
  }

  @Test
  public void testCleanedEmptyIsDiscarded() {
    source.addColumn(new Column("?uri", "ID", null, null));
    source.addColumn(new Column("http://b", "PROP", null,
                                new EmptyCleaner()));
    
    RecordIterator it = read("<http://a> <http://b> \"foo\" .\n");

    assertFalse(it.hasNext());
  }
  
  @Test
  public void testEmptyRecord() {
    source.addColumn(new Column("?uri", "ID", null, null));
    source.addColumn(new Column("http://b", "PROP", null, null));
    
    RecordIterator it = read("<http://a> <http://c> \"foo\" .\n");

    assertFalse("failed to filter out empty records",
                it.hasNext());
  }
  
  // --- helpers
  
  private RecordIterator read(String csvdata) {
    source.setReader(new StringReader(csvdata));
    return source.getRecords();
  }

  static class NullCleaner implements Cleaner {
    public String clean(String value) {
      return null;
    }
  }

  static class EmptyCleaner implements Cleaner {
    public String clean(String value) {
      return "";
    }
  }
}