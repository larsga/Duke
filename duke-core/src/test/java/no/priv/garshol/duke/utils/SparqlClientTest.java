
package no.priv.garshol.duke.utils;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.xml.sax.InputSource;

import static junit.framework.Assert.assertEquals;

public class SparqlClientTest {

  @Test
  public void testEmpty() throws IOException {
    SparqlResult result = load("sparql-empty.xml");
    assertEquals(0, result.getRows().size());
    assertEquals(0, result.getVariables().size());
  }

  @Test
  public void testOneRow() throws IOException {
    SparqlResult result = load("sparql-onerow.xml");

    assertEquals(1, result.getVariables().size());
    assertEquals("x", result.getVariables().get(0));
    
    assertEquals(1, result.getRows().size());
    String[] row = result.getRows().get(0);
    assertEquals(1, row.length);
    assertEquals("1", row[0]);
  }

  @Test
  public void testOneRow2Col() throws IOException {
    SparqlResult result = load("sparql-onerow2col.xml");

    assertEquals(2, result.getVariables().size());
    assertEquals("x", result.getVariables().get(0));
    assertEquals("y", result.getVariables().get(1));
    
    assertEquals(1, result.getRows().size());
    String[] row = result.getRows().get(0);
    assertEquals(2, row.length);
    assertEquals("1", row[0]);
    assertEquals("http://example.org", row[1]);
  }

  @Test
  public void testTwoRow2Col() throws IOException {
    SparqlResult result = load("sparql-tworow2col.xml");

    assertEquals(2, result.getVariables().size());
    assertEquals("x", result.getVariables().get(0));
    assertEquals("y", result.getVariables().get(1));

    List<String[]> results = result.getRows();
    assertEquals(2, results.size());
    String[] row = results.get(0);
    assertEquals(2, row.length);
    assertEquals("1", row[0]);
    assertEquals("http://example.org", row[1]);
    row = results.get(1);
    assertEquals(2, row.length);
    assertEquals("2", row[0]);
    assertEquals("http://example.com", row[1]);    
  }

  @Test
  public void testTwoRow2ColInconsistent() throws IOException {
    SparqlResult result = load("sparql-tworow2col-inconsistent.xml");

    assertEquals(2, result.getVariables().size());
    assertEquals("x", result.getVariables().get(0));
    assertEquals("y", result.getVariables().get(1));

    List<String[]> results = result.getRows();
    assertEquals(2, results.size());
    String[] row = results.get(0);
    assertEquals(2, row.length);
    assertEquals("1", row[0]);
    assertEquals("http://example.org", row[1]);
    row = results.get(1);
    assertEquals(2, row.length);
    assertEquals("2", row[0]);
    assertEquals("http://example.com", row[1]);    
  }

  @Test
  public void testBnode() throws IOException {
    SparqlResult result = load("sparql-bnode.xml");
 
    assertEquals(1, result.getVariables().size());
    assertEquals("x", result.getVariables().get(0));
    
    assertEquals(1, result.getRows().size());
    String[] row = result.getRows().get(0);
    assertEquals(1, row.length);
    assertEquals("r2", row[0]);
 }
  
  private SparqlResult load(String file) throws IOException {
    return SparqlClient.loadResultSet(getStream(file));
  }
  
  private InputSource getStream(String file) throws IOException {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    return new InputSource(cl.getResourceAsStream(file));
  }
}