
package no.priv.garshol.duke.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

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

  @Test
  public void testBlankNode() throws IOException {
    writer.statement("_:foo", "http://b", "foo", true);
    writer.done();
    model = getModel();
    assertEquals(1, model.size());

    NTriplesParserTest.Statement stmt = model.get(0);
    assertEquals("_:foo", stmt.subject);
    assertEquals("http://b", stmt.property);
    assertEquals("foo", stmt.object);
    assertEquals(true, stmt.literal);
  }

  @Test
  public void testNonAscii() throws IOException {
    writer.statement("http://a", "http://b", "f\u00d8\u00d8", true);
    writer.done();
    model = getModel();
    assertEquals(1, model.size());

    NTriplesParserTest.Statement stmt = model.get(0);
    assertEquals("http://a", stmt.subject);
    assertEquals("http://b", stmt.property);
    assertEquals("f\u00d8\u00d8", stmt.object);
    assertEquals(true, stmt.literal);
  }

  @Test
  public void testReallyNonAscii() throws IOException {
    writer.statement("http://a", "http://b", "yi syllable tuox: \uA126", true);
    writer.done();
    model = getModel();
    assertEquals(1, model.size());

    NTriplesParserTest.Statement stmt = model.get(0);
    assertEquals("http://a", stmt.subject);
    assertEquals("http://b", stmt.property);
    assertEquals("yi syllable tuox: \uA126", stmt.object);
    assertEquals(true, stmt.literal);
  }

  @Test
  public void testEscapingQuote() throws IOException {
    writer.statement("http://a", "http://b", "oida\"", true);
    writer.done();
    model = getModel();
    assertEquals(1, model.size());

    NTriplesParserTest.Statement stmt = model.get(0);
    assertEquals("http://a", stmt.subject);
    assertEquals("http://b", stmt.property);
    assertEquals("oida\"", stmt.object);
    assertEquals(true, stmt.literal);
  }

  @Test
  public void testEscapingNewline() throws IOException {
    writer.statement("http://a", "http://b", "oida\nhuff", true);
    writer.done();
    model = getModel();
    assertEquals(1, model.size());

    NTriplesParserTest.Statement stmt = model.get(0);
    assertEquals("http://a", stmt.subject);
    assertEquals("http://b", stmt.property);
    assertEquals("oida\nhuff", stmt.object);
    assertEquals(true, stmt.literal);
  }

  @Test
  public void testEscapingBackslash() throws IOException {
    writer.statement("http://a", "http://b", "oida\\huff", true);
    writer.done();
    model = getModel();
    assertEquals(1, model.size());

    NTriplesParserTest.Statement stmt = model.get(0);
    assertEquals("http://a", stmt.subject);
    assertEquals("http://b", stmt.property);
    assertEquals("oida\\huff", stmt.object);
    assertEquals(true, stmt.literal);
  }
  
  private List<NTriplesParserTest.Statement> getModel() throws IOException {
    return NTriplesParserTest.parse(out.toString());
  }
}