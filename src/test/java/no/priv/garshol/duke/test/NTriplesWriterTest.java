
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import junit.framework.AssertionFailedError;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.StringReader;
import java.io.ByteArrayOutputStream;

import no.priv.garshol.duke.utils.NTriplesParser;
import no.priv.garshol.duke.utils.NTriplesWriter;

public class NTriplesWriterTest {
  private ByteArrayOutputStream out;
  private NTriplesWriter writer;
  private List<NTriplesParserTest.Statement> model;

  @Before
  public void setup() {
    out = new ByteArrayOutputStream();
    writer = new NTriplesWriter(out);
  }
  
  @Test
  public void testEmpty() throws IOException {
    writer.done();
    model = getModel();
    assertTrue("empty data didn't produce empty model", model.isEmpty());
  }

  @Test
  public void testOneStatement() throws IOException {
    writer.statement("http://a", "http://b", "foo", true);
    writer.done();
    model = getModel();
    assertEquals(1, model.size());

    NTriplesParserTest.Statement stmt = model.get(0);
    assertEquals("http://a", stmt.subject);
    assertEquals("http://b", stmt.property);
    assertEquals("foo", stmt.object);
    assertEquals(true, stmt.literal);
  }

  @Test
  public void testOneStatementURI() throws IOException {
    writer.statement("http://a", "http://b", "http://c", false);
    writer.done();
    model = getModel();
    assertEquals(1, model.size());

    NTriplesParserTest.Statement stmt = model.get(0);
    assertEquals("http://a", stmt.subject);
    assertEquals("http://b", stmt.property);
    assertEquals("http://c", stmt.object);
    assertEquals(false, stmt.literal);
  }
  
  private List<NTriplesParserTest.Statement> getModel() throws IOException {
    return NTriplesParserTest.parse(out.toString());
  }
}