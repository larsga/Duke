
package no.priv.garshol.duke.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.StatementHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NTriplesParserTest {

  @Test
  public void testEmpty() throws IOException {
    List<Statement> model = parse("");
    assertTrue("empty data didn't produce empty model", model.isEmpty());
  }

  @Test
  public void testSingleLine() throws IOException {
    List<Statement> model = parse("<http://a> <http://b> <http://c> .");
    assertEquals(1, model.size());
    Statement st = model.get(0);
    assertEquals("subject", "http://a", st.subject);
    assertEquals("property", "http://b", st.property);
    assertEquals("object", "http://c", st.object);
    assertEquals("literal", false, st.literal);
  }

  @Test
  public void testSingleLineBnode() throws IOException {
    List<Statement> model = parse("_:a <http://b> <http://c> .");
    assertEquals(1, model.size());
    Statement st = model.get(0);
    assertEquals("subject", "_:a", st.subject);
    assertEquals("property", "http://b", st.property);
    assertEquals("object", "http://c", st.object);
    assertEquals("literal", false, st.literal);
  }

  @Test
  public void testTwoLines() throws IOException {
    List<Statement> model = parse("<http://a> <http://b> <http://c> .\n" +
                                  "<http://d> <http://e> <http://f> .\n");
    assertEquals(2, model.size());

    Statement st = model.get(0);
    assertEquals("subject", "http://a", st.subject);
    assertEquals("property", "http://b", st.property);
    assertEquals("object", "http://c", st.object);
    assertEquals("literal", false, st.literal);

    st = model.get(1);
    assertEquals("subject", "http://d", st.subject);
    assertEquals("property", "http://e", st.property);
    assertEquals("object", "http://f", st.object);
    assertEquals("literal", false, st.literal);
  }

  @Test
  public void testSingleLineLiteral() throws IOException {
    List<Statement> model = parse("<http://a> <http://b> \"c\" .");
    assertEquals(1, model.size());
    Statement st = model.get(0);
    assertEquals("subject", "http://a", st.subject);
    assertEquals("property", "http://b", st.property);
    assertEquals("object", "c", st.object);
    assertEquals("literal", true, st.literal);
  }

  @Test
  public void testLiteralEscaping() throws IOException {
    List<Statement> model = parse("<http://data.semanticweb.org/person/antonella-poggi> <http://data.semanticweb.org/ns/swc/ontology#affiliation> \"Universita' degli Studi di Roma 'La \\r\\n    Sapienza'\" .  ");
    assertEquals(1, model.size());
    Statement st = model.get(0);
    assertEquals("subject", "http://data.semanticweb.org/person/antonella-poggi", st.subject);
    assertEquals("property", "http://data.semanticweb.org/ns/swc/ontology#affiliation", st.property);
    assertEquals("object", "Universita' degli Studi di Roma 'La \r\n    Sapienza'", st.object);
    assertEquals("literal", true, st.literal);
  }

  @Test
  public void testComment() throws IOException {
    List<Statement> model = parse("<http://a> <http://b> <http://c> .\n" +
                                  "# this is a comment\n" +
                                  "<http://d> <http://e> <http://f> .\n");
    assertEquals(2, model.size());

    Statement st = model.get(0);
    assertEquals("subject", "http://a", st.subject);
    assertEquals("property", "http://b", st.property);
    assertEquals("object", "http://c", st.object);
    assertEquals("literal", false, st.literal);

    st = model.get(1);
    assertEquals("subject", "http://d", st.subject);
    assertEquals("property", "http://e", st.property);
    assertEquals("object", "http://f", st.object);
    assertEquals("literal", false, st.literal);
  }

  @Test
  public void testLanguageTag() throws IOException {
    List<Statement> model = parse("<http://a> <http://b> \"foo\"@en .");
    assertEquals(1, model.size());
    Statement st = model.get(0);
    assertEquals("subject", "http://a", st.subject);
    assertEquals("property", "http://b", st.property);
    assertEquals("object", "foo", st.object);
    assertEquals("literal", true, st.literal);
  }

  @Test
  public void testLanguageTagLonger() throws IOException {
    List<Statement> model = parse("<http://a> <http://b> \"foo\"@en-uk .");
    assertEquals(1, model.size());
    Statement st = model.get(0);
    assertEquals("subject", "http://a", st.subject);
    assertEquals("property", "http://b", st.property);
    assertEquals("object", "foo", st.object);
    assertEquals("literal", true, st.literal);
  }

  @Test
  public void testLanguageTagBad() throws IOException {
    try {
      parse("<http://a> <http://b> \"foo\"@12 .");
      fail("bad language tag accepted");
    } catch (DukeException e) {
      // we detected the bad language tag
    }
  }

  @Test
  public void testLanguageTagBad2() throws IOException {
    try {
      parse("<http://a> <http://b> \"foo\"@en-gb-uk .");
      fail("bad language tag accepted");
    } catch (DukeException e) {
      // we detected the bad language tag
    }
  }
  
  @Test
  public void testDataType() throws IOException {
    List<Statement> model = parse("<http://a> <http://b> \"1\"^^<http://www.w3.org/2001/XMLSchema#int> .");
    assertEquals(1, model.size());
    Statement st = model.get(0);
    assertEquals("subject", "http://a", st.subject);
    assertEquals("property", "http://b", st.property);
    assertEquals("object", "1", st.object);
    assertEquals("literal", true, st.literal);
  }

  @Test
  public void testDataTypeAndLanguageTag() throws IOException {
    try {
      parse("<http://a> <http://b> \"1\"^^<http://www.w3.org/2001/XMLSchema#int>@en .");
      fail("language tag AND datatype not allowed");
    } catch (DukeException e) {
      // error detected
    }
  }

  @Test
  public void testLanguageTagAndDataType() throws IOException {
    try {
      parse("<http://a> <http://b> \"1\"@en^^<http://www.w3.org/2001/XMLSchema#int> .");
      fail("language tag AND datatype not allowed");
    } catch (DukeException e) {
      // error detected
    }
  }
  
  @Test
  public void testLiteralEscaping2() throws IOException {
    String aelig = "\\u" + "00C6"; // doing this to avoid Java parser issues
    List<Statement> model = parse("<http://data.semanticweb.org/a> <http://data.semanticweb.org/b> \"\\\\\\\"" + aelig + "bing\" .  ");
    assertEquals(1, model.size());
    Statement st = model.get(0);
    assertEquals("subject", "http://data.semanticweb.org/a", st.subject);
    assertEquals("property", "http://data.semanticweb.org/b", st.property);
    assertEquals("object", "\\\"\u00C6bing", st.object);
    assertEquals("literal", true, st.literal);
  }

  @Test // checks lower-case letters in escape sequences
  public void testLiteralEscaping3() throws IOException {
    String aelig = "\\u" + "00c6"; // doing this to avoid Java parser issues
    List<Statement> model = parse("<http://data.semanticweb.org/a> <http://data.semanticweb.org/b> \"\\\\\\\"" + aelig + "bing\" .  ");
    assertEquals(1, model.size());
    Statement st = model.get(0);
    assertEquals("subject", "http://data.semanticweb.org/a", st.subject);
    assertEquals("property", "http://data.semanticweb.org/b", st.property);
    assertEquals("object", "\\\"\u00C6bing", st.object);
    assertEquals("literal", true, st.literal);
  }
  
  @Test
  public void testBlankLine() throws IOException {
    List<Statement> model = parse("<http://a> <http://b> <http://c> .\n\n" +
                                  "<http://d> <http://e> <http://f> .\n");
    assertEquals(2, model.size());

    Statement st = model.get(0);
    assertEquals("subject", "http://a", st.subject);
    assertEquals("property", "http://b", st.property);
    assertEquals("object", "http://c", st.object);
    assertEquals("literal", false, st.literal);

    st = model.get(1);
    assertEquals("subject", "http://d", st.subject);
    assertEquals("property", "http://e", st.property);
    assertEquals("object", "http://f", st.object);
    assertEquals("literal", false, st.literal);
  }

  @Test
  public void testSingleLineFollowedByGarbage() throws IOException {
    try {
      parse("<http://a> <http://b> <http://c> . <http://d> ");
      fail("parser accepted invalid data");
    } catch (DukeException e) {
    }
  }

  @Test
  public void testLineStops() throws IOException {
    try {
      parse("<http://data.mattilsynet.no/sesam/webcruiter/dokument/8865432a-0eec-41e2-a781-992091aba0cc>	  ");
      fail("parser accepted invalid data");
    } catch (DukeException e) {
    }
  }

  @Test
  public void testLineStopsBeforeObject() throws IOException {
    try {
      parse("<http://data.mattilsynet.no/sesam/webcruiter/dokument/8865432a-0eec-41e2-a781-992091aba0cc>	  <http://foo> ");
      fail("parser accepted invalid data");
    } catch (DukeException e) {
    }
  }

  @Test
  public void testLineStopsBeforePeriod() throws IOException {
    try {
      parse("<http://data.mattilsynet.no/sesam/webcruiter/dokument/8865432a-0eec-41e2-a781-992091aba0cc>	  <http://foo> \"2\" ");
      fail("parser accepted invalid data");
    } catch (DukeException e) {
    }
  }

  @Test
  public void testLineStopsInURI() throws IOException {
    try {
      parse("<http://data.mattilsynet.no/sesam/webcruiter/dokument/8865432a-0eec-41e2-a781-992091aba0cc>	  <http://foo> <http://burrur");
      fail("parser accepted invalid data");
    } catch (DukeException e) {
    }
  }

  public static List<Statement> parse(String data) throws IOException {
    StatementBuilder builder = new StatementBuilder();
    NTriplesParser.parse(new StringReader(data), builder);
    return builder.statements;
  }

  // --- Recording statement handler

  static class Statement {
    String subject;
    String property;
    String object;
    boolean literal;

    public Statement(String subject, String property, String object,
                     boolean literal) {
      this.subject = subject;
      this.property = property;
      this.object = object;
      this.literal = literal;
    }
  }

  static class StatementBuilder implements StatementHandler {
    List<Statement> statements;

    public StatementBuilder() {
      this.statements = new ArrayList();
    }

    public void statement(String subject, String property, String object,
                          boolean literal) {
      statements.add(new Statement(subject, property, object, literal));
    }
  }
}
