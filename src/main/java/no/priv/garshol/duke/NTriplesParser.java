
package no.priv.garshol.duke;

import java.io.Reader;
import java.io.IOException;
import java.io.BufferedReader;

/**
 * A basic NTriples parser used by NTriplesDataSource.
 */
public class NTriplesParser {

  public static void parse(Reader src, StatementHandler handler)
    throws IOException {
    BufferedReader in = new BufferedReader(src);
    String line = in.readLine();
    while (line != null) {
      parseLine(line, handler);
      line = in.readLine();
    }    
  }

  private static void parseLine(String line, StatementHandler handler) {
    int pos = skipws(line, 0);
    if (line.charAt(pos) != '<')
      throw new RuntimeException("Subject does not start with '<'");

    int start = pos + 1; // skip initial '<'
    pos = parseuri(line, pos);
    String subject = line.substring(start, pos);

    pos = skipws(line, pos + 1); // skip '>'
    if (line.charAt(pos) != '<')
      throw new RuntimeException("Predicate does not start with '<', " +
                                 "nearby: '" +
                                 line.substring(pos - 5, pos + 5) + "', at " +
                                 "position: " + pos);

    start = pos + 1; // skip initial '<'
    pos = parseuri(line, pos);
    String property = line.substring(start, pos);
    
    pos = skipws(line, pos + 1); // skip '>'

    start = pos + 1; // skip initial '<' or '"'
    boolean literal = false;
    String object;
    if (line.charAt(pos) == '<')
      pos = parseuri(line, pos);
    else if (line.charAt(pos) == '"') {
      pos = parseliteral(line, pos + 1);
      literal = true;
    } else
      throw new RuntimeException("Object didn't start with '\"' or '<'");

    object = line.substring(start, pos);
    if (literal)
      object = unescape(object);
    pos++; // skip terminator
    pos = skipws(line, pos);
    if (line.charAt(pos) != '.')
      throw new RuntimeException("Statement did not end with period; line: '" +
                                 line + "'");

    handler.statement(subject, property, object, literal);
  }

  private static String unescape(String literal) {
    char[] buf = new char[literal.length()];
    int pos = 0;

    for (int ix = 0; ix < literal.length(); ix++)
      if (literal.charAt(ix) == '\\') {
        ix++;
        char ch = literal.charAt(ix);
        if (ch == 'n')
          buf[pos++] = '\n';
        else if (ch == 'r')
          buf[pos++] = '\r';
        else if (ch == 't')
          buf[pos++] = '\t';
        else if (ch == '\\')
          buf[pos++] = '\\';
        else if (ch == '"')
          buf[pos++] = '"';
        else
          throw new RuntimeException("Unknown escaped character: '" + ch + "' in '" + literal + "'");
      } else
        buf[pos++] = literal.charAt(ix);

    return new String(buf, 0, pos);
  }
  
  private static int parseuri(String line, int pos) {
    while (line.charAt(pos) != '>')
      pos++;
    return pos;
  }

  private static int parseliteral(String line, int pos) {
    while (line.charAt(pos) != '"') {
      if (line.charAt(pos) == '\\')
        pos++; // skip escaped char (we decode later)
      pos++;
    }
    return pos;
  }

  private static int skipws(String line, int pos) {
    while (pos < line.length()) {
      char ch = line.charAt(pos);
      if (!(ch == ' ' || ch == '\t'))
        break;
      pos++;
    }
    return pos;
  }

  /**
   * Event-handler which receives parsed statements.
   */
  public interface StatementHandler {
    public void statement(String subject, String property, String object,
                          boolean literal);
  }
  
}