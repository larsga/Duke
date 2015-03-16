
package no.priv.garshol.duke.utils;

import java.util.Stack;
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.ParserFactory;

import no.priv.garshol.duke.DukeException;

/**
 * A SAX document handler that writes pretty-printed XML to a Writer.
 * Copied from the Ontopia source code.
 */
public class XMLPrettyPrinter implements DocumentHandler {
  private static final String NL = System.getProperty("line.separator");
  
  protected Writer writer;
  protected String encoding;
  protected boolean[] subelements;
  protected char[] startline; // contains "\n      ...", used for indents
  protected int offset; // width of newline (1-2 chars)
  protected int level; // current element depth

  protected int encodeCharsFrom = -1;
  protected boolean dropControlChars = true; // drop control chars by default

  /**
   * Creates an XMLPrettyPrinter that writes to the given OutputStream.
   * The encoding used is always utf-8.
   */
  public XMLPrettyPrinter(OutputStream stream) throws UnsupportedEncodingException {
    this(stream, "utf-8");
  }

  /**
   * Creates an XMLPrettyPrinter that writes to the given OutputStream
   * in the requested character encoding.
   */
  public XMLPrettyPrinter(OutputStream stream, String encoding) throws UnsupportedEncodingException {
    this(new OutputStreamWriter(stream, encoding), encoding);
  }

  /**
   * Creates an XMLPrettyPrinter that writes to the given Writer.
   * @param encoding The encoding to report in the XML declaration. If null,
   * no XML declaration will be output.
   */
  public XMLPrettyPrinter(Writer writer, String encoding) {
    this.writer = writer;
    this.encoding = encoding;
    makeSubelements(20);
    makeStartLineBuffer(100);
  }  
  
  // --------------------------------------------------------------------------
  // Document events
  // --------------------------------------------------------------------------
  
  public void startDocument() {
    if (encoding != null) {
      write(writer, "<?xml version=\"1.0\" encoding=\"");
      write(writer, encoding);
      write(writer, "\" standalone=\"yes\"?>" + NL);
    }
    level = 0;
  }

  public void startElement(String name, AttributeList atts) {
    if (level > 0)
      indent();
    // Write start tag
    write(writer, '<');
    write(writer, name);
    if (atts != null) {
      for (int i = 0; i < atts.getLength(); i++) {
        write(writer, ' ');
        write(writer, atts.getName(i));
        write(writer, "=\"");
        escapeAttrValue(atts.getValue(i), writer);
        write(writer, "\"");
      }
    }
    write(writer, '>');

    level++;

    // check arrays are still of right size
    if (offset + level*2 > startline.length)
      makeStartLineBuffer((offset + level*2) * 2);
    if (level >= subelements.length)
      makeSubelements(level * 2);
    
    // Set sub element flag on parent element to true
    if (level > 0)
      subelements[level - 1] = true;

    // Set sub element flag on this element to false
    subelements[level] = false;
  }

  public void endElement(String name) {
    if (subelements[level--])
      indent();

    write(writer, "</");
    write(writer, name);
    write(writer, '>');
  }

  public void text(String text) {
    characters(text.toCharArray(), 0, text.length());
  }

  public void characters(char ch[], int start, int length) {
    // Encode characters as decimal character entities
    for (int i = start; i < start + length; i++) {
      switch(ch[i]) {
      case '&':
        write(writer, "&amp;");
        break;
      case '<':
        write(writer, "&lt;");
        break;
      case '>':
        write(writer, "&gt;");
        break;
      default:
        if (ch[i] > 31 || ch[i] == '\n' || ch[i] == '\t' || ch[i] == '\r') {
          if (encodeCharsFrom > 0 && ch[i] >= encodeCharsFrom) {
            write(writer, "&#");
            write(writer, Integer.toString((int)ch[i]));
            write(writer, ';');
          } else {
            write(writer, ch[i]);
          }
        } else {
          if (!dropControlChars) {
            // escape control characters
            write(writer, "&#");
            write(writer, Integer.toString((int)ch[i]));
            write(writer, ';');
          }
        }
      }      
    }
  }

  public void ignorableWhitespace(char ch[], int start, int length) {
  }

  public void processingInstruction(String target, String data) {
    // Write processing instruction
    write(writer, "<?");
    write(writer, target);
    write(writer, ' ');
    write(writer, data);
    write(writer, "?>");
  }

  public void endDocument() {
    write(writer, NL);
    flush(writer);
  }

  public void setDocumentLocator(Locator locator) {
  }

  /**
   * INTERNAL: Encodes element content as decimal character entitites
   * for characters from the given character number.
   */
  public void setEncodeCharactersFrom(int charnumber) {
    this.encodeCharsFrom = charnumber;
  }

  /**
   * INTERNAL: If this property is true control characters are being
   * dropped from the resulting document.
   */
  public void setDropControlCharacters(boolean dropControlChars) {
    this.dropControlChars = dropControlChars;
  }

  /**
   * INTERNAL: Add given text unmodified and unescaped to the output.
   * <b>BEWARE:</b> This makes it possible (even easy) to produce
   * output that is not well-formed.
   */
  public void addUnescaped(String content) {
    write(writer, content);
  }
  
  // --------------------------------------------------------------------------
  // Helper methods
  // --------------------------------------------------------------------------

  protected void write(Writer writer, String s) {
    try {
      writer.write(s);
    } catch (IOException e) {
      throw new DukeException(e);
    }
  }

  protected void write(Writer writer, char c) {
    try {
      writer.write(c);
    } catch (IOException e) {
      throw new DukeException(e);
    }
  }

  protected void write(Writer writer, char[] c, int off, int len) {
    try {
      writer.write(c, off, len);
    } catch (IOException e) {
      throw new DukeException(e);
    }
  }

  protected void flush(Writer writer) {
    try {
      writer.flush();
    } catch (IOException e) {
      throw new DukeException(e);
    }
  }
  
  protected void indent() {
    write(writer, startline, 0, offset + level * 2);
  }

  protected void escapeAttrValue(String attrval, Writer writer) {
    int len = attrval.length();
    for (int i=0; i < len; i++) {
      char c = attrval.charAt(i);
      switch(c) {
      case '&':
        write(writer, "&amp;");
        break;
      case '<':
        write(writer, "&lt;");
        break;
      case '"':
        write(writer, "&quot;");
        break;
      default:
        if (c > 31 || c == '\n' || c == '\t' || c == '\r') {
          if (encodeCharsFrom > 0 && c >= encodeCharsFrom) {
            write(writer, "&#");
            write(writer, Integer.toString((int)c));
            write(writer, ';');
          } else {
            write(writer, c);
          }
        } else {
          if (!dropControlChars) {
            // escape control characters
            write(writer, "&#");
            write(writer, Integer.toString((int)c));
            write(writer, ';');
          }
        }
      }
    }
  }

  protected void makeStartLineBuffer(int size) {
    startline = new char[size];
    offset = NL.length();
    int ix = 0;
    for (; ix < offset; ix++)
      startline[ix] = NL.charAt(ix);

    for (; ix < size; ix++)
      startline[ix] = ' ';
  }

  protected void makeSubelements(int size) {
    boolean subs[] = new boolean[size];
    if (subelements != null)
      System.arraycopy(subelements, 0, subs, 0, subelements.length);
    subelements = subs;
  }
  
}
