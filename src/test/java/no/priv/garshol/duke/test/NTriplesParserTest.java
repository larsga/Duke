
package no.priv.garshol.duke.test;

import org.junit.Test;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import junit.framework.AssertionFailedError;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.StringReader;
import no.priv.garshol.duke.NTriplesParser;

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

  private static List<Statement> parse(String data) throws IOException {
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

  static class StatementBuilder implements NTriplesParser.StatementHandler {
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