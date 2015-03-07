
package no.priv.garshol.duke.utils;

import java.io.Reader;
import java.io.IOException;
import java.io.BufferedReader;

import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.StatementHandler;

/**
 * A basic NTriples parser used by NTriplesDataSource.
 */
public class NTriplesParser {
  private Reader src;
  private StatementHandler handler;
  private int lineno;
  private int pos;
  private String line;

  /**
   * Reads the NTriples file from the reader, pushing statements into
   * the handler.
   */
  public static void parse(Reader src, StatementHandler handler)
    throws IOException {
    new NTriplesParser(src, handler).parse();
  }

  private NTriplesParser(Reader src, StatementHandler handler) {
    this.src = src;
    this.handler = handler;
  }

  /**
   * Alternate entry point to the parser for when the driving loop is
   * outside the parser. Statements get passed to the handler.
   */
  public NTriplesParser(StatementHandler handler) {
    this(null, handler);
  }

  /**
   * Push a line into the parser. If it contains a statement, that
   * statement will be passed to the handler.
   */
  public void parseLine(String line) {
    this.line = line;
    parseLine();
  }

  private void parse() throws IOException {
    BufferedReader in = new BufferedReader(src);
    line = in.readLine();
    while (line != null) {
      lineno++;
      parseLine();
      line = in.readLine();
    }    
  }

  private void parseLine() {
    pos = 0;
    skipws();
    if (pos >= line.length() || line.charAt(pos) == '#')
      return; // think there's nothing to do in this case

    // subject
    String subject;
    if (line.charAt(pos) == '<')
      subject = parseuri();
    else if (line.charAt(pos) == '_')
      subject = parsebnode();
    else
      throw new DukeException("Subject in line " + lineno +
                              " is neither URI nor bnode: " + line);

    skipws();

    // property
    if (pos >= line.length())
      throw new DukeException("Line ends before predicate on line " + lineno);
    else if (line.charAt(pos) != '<')
      throw new DukeException("Predicate does not start with '<', " +
                              "nearby: '" +
                              line.substring(pos - 5, pos + 5) + "', at " +
                              "position: " + pos + " in line " + lineno);
    String property = parseuri();

    skipws();

    // object
    boolean literal = false;
    String object;
    if (pos >= line.length())
      throw new DukeException("Line ends before object on line " + lineno);
    else if (line.charAt(pos) == '<')
      object = parseuri();
    else if (line.charAt(pos) == '"') {
      object = unescape(parseliteral());
      literal = true;
    } else if (line.charAt(pos) == '_')
      object = parsebnode();
    else
      throw new DukeException("Illegal object on line " + lineno + ": " +
                              line.substring(pos));

    // terminator
    skipws();
    if (pos >= line.length() || line.charAt(pos++) != '.')
      throw new DukeException("Statement did not end with period; line: '" +
                              line + "', line number: " + lineno);

    skipws();
    if (pos + 1 < line.length())
      throw new DukeException("Garbage after period on line " + lineno);
    
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
        else if (ch == 'u') {
          ix++; // step over the 'u'
          if (literal.length() < ix + 4 ||
              !(hexchar(literal.charAt(ix)) &&
                hexchar(literal.charAt(ix + 1)) &&
                hexchar(literal.charAt(ix + 2)) &&
                hexchar(literal.charAt(ix + 3))))
            throw new DukeException("Bad Unicode escape: '" +
                                    literal.substring(ix - 2, ix + 4) + "'");
          buf[pos++] = unhex(literal, ix);
          ix += 3;
        } else
          throw new DukeException("Unknown escaped character: '" + ch + "' in '" + literal + "'");
      } else
        buf[pos++] = literal.charAt(ix);

    return new String(buf, 0, pos);
  }

  private static boolean hexchar(char ch) {
    return (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F') ||
           (ch >= 'a' && ch <= 'f');
  }

  private static char unhex(String literal, int pos) {
    int charno = 0;
    for (int ix = pos; ix < pos + 4; ix++) {
      int digit;
      char ch = literal.charAt(ix);
      if (ch >= '0' && ch <= '9')
        digit = ch - '0';
      else if (ch >= 'a' && ch <= 'f')
        digit = (ch - 'a') + 10;
      else
        digit = (ch - 'A') + 10;
      charno = (charno * 16) + digit;
    }
    return (char) charno;
  }
  
  private String parseuri() {
    int start = pos + 1; // skip initial '<'
    while (pos < line.length() && line.charAt(pos) != '>')
      pos++;
    if (pos >= line.length())
      throw new DukeException("Line ends in URI at line " + lineno);
    pos++; // skip final '>'
    return line.substring(start, pos - 1);
  }

  private String parseliteral() {
    pos++; // skip initial quote
    int start = pos; 
    while (line.charAt(pos) != '"') {
      if (line.charAt(pos) == '\\')
        pos++; // skip escaped char (we decode later)
      pos++;
    }
    int end = pos;
    pos++; // skip final quote

    if (line.charAt(pos) == '^')
      parsedatatype();
    else if (line.charAt(pos) == '@')
      parselangtag();
    
    return line.substring(start, end);
  }

  private void parsedatatype() {
    pos++; // skip first ^
    if (line.charAt(pos++) != '^')
      throw new DukeException("Incorrect start of datatype");
    if (line.charAt(pos) != '<')
      throw new DukeException("Datatype URI does not start with '<'");
    parseuri();
  }

  private void parselangtag() {
    pos++; // skip the '@'
    char ch = line.charAt(pos);
    while ((ch >= 'a' && ch <= 'z') ||
           (ch >= 'A' && ch <= 'Z')) 
      ch = line.charAt(++pos);

    if (line.charAt(pos) != '-')
      return;
    pos++; // consume '-'
    
    ch = line.charAt(pos);
    while ((ch >= 'a' && ch <= 'z') ||
           (ch >= 'A' && ch <= 'Z') ||
           (ch >= '0' && ch <= '9')) 
      ch = line.charAt(pos++);
  }

  private String parsebnode() {
    int start = pos;

    pos++; // skip '_'
    if (line.charAt(pos++) != ':')
      throw new DukeException("Incorrect start of blank node");

    char ch = line.charAt(pos++);
    while ((ch >= 'A' && ch <= 'Z') ||
           (ch >= 'a' && ch <= 'z') ||
           (ch >= '0' && ch <= '9')) 
      ch = line.charAt(pos++);

    return line.substring(start, pos - 1);
  }

  private void skipws() {
    while (pos < line.length()) {
      char ch = line.charAt(pos);
      if (!(ch == ' ' || ch == '\t'))
        break;
      pos++;
    }
  }
}
