
package no.priv.garshol.duke.datasources;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.utils.SparqlResult;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class SparqlDataSourceTest {
  private PagedTestSparqlDataSource source;
  private static final String RDF_TYPE =
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

  @Before
  public void setup() {
    source = new PagedTestSparqlDataSource();
    source.setEndpoint("http://localhost:8890/sparql");
    source.setQuery("select * { where ?s ?p ?o }");
  }

  // ===== GENERIC TESTS
  
  @Test
  public void testEmpty() {
    source.setVariables(new String[] {});
    source.setRows(new String[][] {});
    RecordIterator it = source.getRecords();
    assertFalse("empty data source contains records",
                it.hasNext());
    assertEquals("wrong number of pages", 1, source.getPages());
  }

  // ===== TRIPLE MODE ERROR CHECKING
  
  @Test
  public void testTripleModeNoUri() {
    setupTripleConfig1("?url"); // NOTE: url, not uri
    source.setRows(new String[][] {{"http://a/1", "http://a/name", "1"}});
    
    try {
      RecordIterator it = source.getRecords();
      it.next();
      fail("No config exception, despite missing '?uri' column");
    } catch (DukeConfigException e) {
    }
  }

  @Test
  public void testTripleModeFourColumns() {
    source.addColumn(new Column("?uri", "ID", null, null));
    source.addColumn(new Column("http://a/name", "NAME", null, null));
    source.addColumn(new Column("http://a/age", "AGE", null, null));
    source.addColumn(new Column("http://a/blubb", "BLUBB", null, null));

    source.setVariables(new String[] {"S", "P", "O", "C"});
    source.setRows(new String[][] {
        {"http://a/1", "http://a/name", "1", "blubb"}});

    try {
      RecordIterator it = source.getRecords();
      it.next();
      fail("Didn't catch four result columns in triple mode");
    } catch (DukeConfigException e) {
    }
  }
  
  // ===== TRIPLE MODE, CONFIG 1
  
  private void setupTripleConfig1(String urlcol) {
    source.addColumn(new Column(urlcol, "ID", null, null));
    source.addColumn(new Column("http://a/name", "NAME", null, null));
    source.addColumn(new Column("http://a/age", "AGE", null, null));

    source.setVariables(new String[] {"S", "P", "O"});
  }
  
  @Test
  public void testSingleTriple() {
    setupTripleConfig1("?uri");
    source.setRows(new String[][] {{"http://a/1", "http://a/name", "1"}});
    
    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "1", r.getValue("NAME"));
    assertEquals("wrong AGE", null, r.getValue("AGE"));

    assertFalse("data source contains more than one record", it.hasNext());
    assertEquals("wrong number of pages", 2, source.getPages());
  }
  
  @Test
  public void testTwoTriples() {
    setupTripleConfig1("?uri");
    source.setRows(new String[][] {
        {"http://a/1", "http://a/name", "1"},
        {"http://a/1", "http://a/age", "32"}});
    
    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "1", r.getValue("NAME"));
    assertEquals("wrong AGE", "32", r.getValue("AGE"));

    assertFalse("data source contains more than one record", it.hasNext());
    assertEquals("wrong number of pages", 2, source.getPages());
  }
  
  @Test
  public void testTwoRecords() {
    setupTripleConfig1("?uri");
    source.setRows(new String[][] {
        {"http://a/1", "http://a/name", "1"},
        {"http://a/1", "http://a/age", "32"},
        {"http://a/2", "http://a/name", "2"},
        {"http://a/2", "http://a/age", "23"}});
    
    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "1", r.getValue("NAME"));
    assertEquals("wrong AGE", "32", r.getValue("AGE"));

    assertTrue("data source contains only one record", it.hasNext());

    r = it.next();
    assertEquals("wrong ID", "http://a/2", r.getValue("ID"));
    assertEquals("wrong NAME", "2", r.getValue("NAME"));
    assertEquals("wrong AGE", "23", r.getValue("AGE"));    
    
    assertFalse("data source contains more than two records", it.hasNext());
    assertEquals("wrong number of pages", 2, source.getPages());
  }
  
  @Test
  public void testTwoRecordsDoubleValue() {
    setupTripleConfig1("?uri");
    source.setRows(new String[][] {
        {"http://a/1", "http://a/name", "1"},
        {"http://a/1", "http://a/name", "one"},
        {"http://a/1", "http://a/age", "32"},
        {"http://a/2", "http://a/name", "2"},
        {"http://a/2", "http://a/age", "23"}});
    
    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));    
    assertEquals("wrong AGE", "32", r.getValue("AGE"));

    Collection<String> values = r.getValues("NAME");
    assertEquals("wrong number of NAMEs", 2, values.size());
    assertTrue("NAMEs doesn't contain '1'", values.contains("1"));
    assertTrue("NAMEs doesn't contain 'one'", values.contains("one"));

    assertTrue("data source contains only one record", it.hasNext());

    r = it.next();
    assertEquals("wrong ID", "http://a/2", r.getValue("ID"));
    assertEquals("wrong NAME", "2", r.getValue("NAME"));
    assertEquals("wrong AGE", "23", r.getValue("AGE"));    
    
    assertFalse("data source contains more than two records", it.hasNext());
    assertEquals("wrong number of pages", 2, source.getPages());
  }
  
  @Test
  public void testTripleMissingColumn() {
    setupTripleConfig1("?uri");
    source.setRows(new String[][] {
        {"http://a/1", "http://a/name", "1"},
        {"http://a/1", "http://a/age", "32"},
        // next column is not configured. Duke should accept it even so
        {"http://a/1", "http://a/wtf", "whee"}});
    
    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "1", r.getValue("NAME"));
    assertEquals("wrong AGE", "32", r.getValue("AGE"));

    assertFalse("data source contains more than one record", it.hasNext());
    assertEquals("wrong number of pages", 2, source.getPages());
  }
  
  @Test
  public void testThreeRecordsPaging1() {
    runThreeRecordsPaging(1, 7);
  }
  
  @Test
  public void testThreeRecordsPaging2() {
    runThreeRecordsPaging(2, 4);
  }
  
  @Test
  public void testThreeRecordsPaging3() {
    runThreeRecordsPaging(3, 3);
  }

  private void runThreeRecordsPaging(int pagesize, int pages) {
    setupTripleConfig1("?uri");
    source.setPageSize(pagesize);
    source.setRows(new String[][] {
        {"http://a/1", "http://a/name", "1"},
        {"http://a/1", "http://a/age", "32"},
        {"http://a/2", "http://a/name", "2"},
        {"http://a/2", "http://a/age", "23"},
        {"http://a/3", "http://a/name", "3"},
        {"http://a/3", "http://a/age", "2323"}});
    
    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "1", r.getValue("NAME"));
    assertEquals("wrong AGE", "32", r.getValue("AGE"));

    assertTrue("data source contains only one record", it.hasNext());

    r = it.next();
    assertEquals("wrong ID", "http://a/2", r.getValue("ID"));
    assertEquals("wrong NAME", "2", r.getValue("NAME"));
    assertEquals("wrong AGE", "23", r.getValue("AGE"));    

    assertTrue("data source contains only two records", it.hasNext());

    r = it.next();
    assertEquals("wrong ID", "http://a/3", r.getValue("ID"));
    assertEquals("wrong NAME", "3", r.getValue("NAME"));
    assertEquals("wrong AGE", "2323", r.getValue("AGE"));    
    
    assertFalse("data source contains more than three records", it.hasNext());
    assertEquals("wrong number of pages", pages, source.getPages());
  }
  
  @Test
  public void testTripleModeNoPaging() {
    setupTripleConfig1("?uri");
    source.setPageSize(0);
    source.setRows(new String[][] {
        {"http://a/1", "http://a/name", "1"},
        {"http://a/1", "http://a/age", "32"},
        {"http://a/2", "http://a/name", "2"},
        {"http://a/2", "http://a/age", "23"}});
    
    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "1", r.getValue("NAME"));
    assertEquals("wrong AGE", "32", r.getValue("AGE"));

    assertTrue("data source contains only one record", it.hasNext());

    r = it.next();
    assertEquals("wrong ID", "http://a/2", r.getValue("ID"));
    assertEquals("wrong NAME", "2", r.getValue("NAME"));
    assertEquals("wrong AGE", "23", r.getValue("AGE"));    
    
    assertFalse("data source contains more than two records", it.hasNext());
    assertEquals("wrong number of pages", 1, source.getPages());
  }
  
  // ===== TABULAR MODE, CONFIG 1
  
  private void setupTabularConfig1() {
    source.setTripleMode(false);
    source.addColumn(new Column("ID", "ID", null, null));
    source.addColumn(new Column("NAME", "NAME", null, null));
    source.addColumn(new Column("AGE", "AGE", null, null));

    source.setVariables(new String[] {"ID", "NAME", "AGE"});
  }
  
  @Test
  public void testSingleRow() {
    setupTabularConfig1();
    source.setRows(new String[][] {{"http://a/1", "Peter", "15"}});
    
    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "Peter", r.getValue("NAME"));
    assertEquals("wrong AGE", "15", r.getValue("AGE"));

    assertFalse("data source contains more than one record", it.hasNext());
    assertEquals("wrong number of pages", 2, source.getPages());
  }
  
  @Test
  public void testTwoRows() {
    setupTabularConfig1();
    source.setRows(new String[][] {
        {"http://a/1", "Peter", "15"},
        {"http://a/2", "George", "151"}});
    
    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "Peter", r.getValue("NAME"));
    assertEquals("wrong AGE", "15", r.getValue("AGE"));

    assertTrue("data source contains only one record", it.hasNext());

    r = it.next();
    assertEquals("wrong ID", "http://a/2", r.getValue("ID"));
    assertEquals("wrong NAME", "George", r.getValue("NAME"));
    assertEquals("wrong AGE", "151", r.getValue("AGE"));    
    
    assertFalse("data source contains more than two records", it.hasNext());
    assertEquals("wrong number of pages", 2, source.getPages());
  }
  
  @Test
  public void testTableModeMissingColumn() {
    setupTabularConfig1();
    // this column is not configured, so Duke should just ignore it
    source.setVariables(new String[] {"ID", "NAME", "AGE", "EXTRA"});
    source.setRows(new String[][] {{"http://a/1", "Peter", "15", "ignore"}});
    
    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "Peter", r.getValue("NAME"));
    assertEquals("wrong AGE", "15", r.getValue("AGE"));

    assertFalse("data source contains more than one record", it.hasNext());
    assertEquals("wrong number of pages", 2, source.getPages());
  }
  
  @Test
  public void testTableModePaging() {
    setupTabularConfig1();
    source.setPageSize(1);
    source.setRows(new String[][] {
        {"http://a/1", "Peter", "15"},
        {"http://a/2", "George", "151"}});

    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "Peter", r.getValue("NAME"));
    assertEquals("wrong AGE", "15", r.getValue("AGE"));

    assertTrue("data source contains only one record", it.hasNext());

    r = it.next();
    assertEquals("wrong ID", "http://a/2", r.getValue("ID"));
    assertEquals("wrong NAME", "George", r.getValue("NAME"));
    assertEquals("wrong AGE", "151", r.getValue("AGE"));    

    assertFalse("data source contains more than two records", it.hasNext());
    assertEquals("wrong number of pages", 3, source.getPages());
  }
  
  @Test
  public void testTableModePaging3() {
    runTableModePageTest(1, 4);
  }
  
  @Test
  public void testTableModePaging3a() {
    runTableModePageTest(2, 3);
  }
  
  @Test
  public void testTableModePaging3b() {
    runTableModePageTest(3, 2);
  }

  private void runTableModePageTest(int pagesize, int pages) {
    setupTabularConfig1();
    source.setPageSize(pagesize);
    source.setRows(new String[][] {
        {"http://a/1", "Peter", "15"},
        {"http://a/2", "George", "151"},
        {"http://a/3", "Fluffy", "25151"}});

    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "Peter", r.getValue("NAME"));
    assertEquals("wrong AGE", "15", r.getValue("AGE"));

    assertTrue("data source contains only one record", it.hasNext());

    r = it.next();
    assertEquals("wrong ID", "http://a/2", r.getValue("ID"));
    assertEquals("wrong NAME", "George", r.getValue("NAME"));
    assertEquals("wrong AGE", "151", r.getValue("AGE"));    

    assertTrue("data source contains only two records", it.hasNext());

    r = it.next();
    assertEquals("wrong ID", "http://a/3", r.getValue("ID"));
    assertEquals("wrong NAME", "Fluffy", r.getValue("NAME"));
    assertEquals("wrong AGE", "25151", r.getValue("AGE"));    
    
    assertFalse("data source contains more than three records", it.hasNext());
    assertEquals("wrong number of pages", pages, source.getPages());
  }
  
  @Test
  public void testTableModeNoPaging() {
    setupTabularConfig1();
    source.setPageSize(0);
    source.setRows(new String[][] {
        {"http://a/1", "Peter", "15"},
        {"http://a/2", "George", "151"}});
    
    RecordIterator it = source.getRecords();
    assertTrue("data source contains no records", it.hasNext());

    Record r = it.next();
    assertEquals("wrong ID", "http://a/1", r.getValue("ID"));
    assertEquals("wrong NAME", "Peter", r.getValue("NAME"));
    assertEquals("wrong AGE", "15", r.getValue("AGE"));

    assertTrue("data source contains only one record", it.hasNext());

    r = it.next();
    assertEquals("wrong ID", "http://a/2", r.getValue("ID"));
    assertEquals("wrong NAME", "George", r.getValue("NAME"));
    assertEquals("wrong AGE", "151", r.getValue("AGE"));    
    
    assertFalse("data source contains more than two records", it.hasNext());
    assertEquals("wrong number of pages", 1, source.getPages());
  }
  
  // ===== UTILITIES

  // regexps for query parsing
  private static final Pattern LIMIT =
    Pattern.compile("\\s+limit\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern OFFSET =
    Pattern.compile("\\s+offset\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
  
  // special overridden version for testing paging
  class PagedTestSparqlDataSource extends SparqlDataSource {
    // this is the columns in the query result
    private String[] vars;
    // this is the complete query result, ignoring limit and offset
    private String[][] rows;
    // used to count the number of pages traversed
    private int pages;

    public void setVariables(String[] vars) {
      this.vars = vars;
    }

    public void setRows(String[][] rows) {
      this.rows = rows;
    }
    
    public SparqlResult runQuery(String endpoint, String query) {
      int limit = parseLimit(query);
      int offset = parseOffset(query);
      if (pagesize == 0)
        // this means paging has been turned off completely
        assertTrue("paging not truly disabled", limit == -1 && offset == -1);

      SparqlResult result = new SparqlResult();
      for (String var : vars)
        result.addVariable(var);

      int end;
      if (limit == -1)
        end = rows.length;
      else
        end = Math.min(offset + limit, rows.length);
      if (offset == -1)
        offset = 0; // we start at zero even so
      for (int ix = offset; ix < end; ix++)
        result.addRow(rows[ix]);

      pages++;
      
      return result;
    }

    /**
     * If we only return a single empty page, this says 1. If we
     * return one page with contents and one without, that's 2. And so
     * on.
     */
    public int getPages() {
      return pages;
    }

    private int parseLimit(String query) {
      Matcher m = LIMIT.matcher(query);
      if (!m.find())
        return -1;
      return Integer.valueOf(m.group(1));
    }

    private int parseOffset(String query) {
      Matcher m = OFFSET.matcher(query);
      if (!m.find())
        return -1;
      return Integer.valueOf(m.group(1));
    }
  }
}