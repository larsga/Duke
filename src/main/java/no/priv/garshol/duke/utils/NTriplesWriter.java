
package no.priv.garshol.duke.utils;

import java.io.Writer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import no.priv.garshol.duke.StatementHandler;

/**
 * A simplified NTriples serializer, used for writing the link file to
 * NTriples format.
 */
public class NTriplesWriter implements StatementHandler {
  private Writer out;

  // using a stream so we can control the encoding
  public NTriplesWriter(OutputStream out) {
    try {
      // the NTriples spec doesn't actually allow anything other than
      // ASCII characters in NTriples files, but we find that DBpedia,
      // for example, violates the RDF and NTriples specs by using
      // IRIs that contain raw non-ASCII characters. so we export to
      // UTF-8 in order not to lose data.
      //
      // http://www.w3.org/TR/rdf-testcases/#absoluteURI
      
      this.out = new OutputStreamWriter(out, "utf-8");
    } catch (java.io.UnsupportedEncodingException e) {
      // can't think of any good reason why this needs to be a checked
      // exception
      throw new RuntimeException(e);
    }
  }

  public void statement(String subject, String property, String object,
                        boolean literal) {
    try {
      if (subject.startsWith("_:"))
        out.write(subject + " ");
      else
        out.write("<" + subject + "> ");
      out.write("<" + property + "> ");

      if (literal)
        out.write('"' + escape(object) + '"' + ' ');
      else if (object.startsWith("_:"))
        out.write(object + " ");
      else
        out.write("<" + object + "> ");

      out.write(".\n");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void done() throws IOException {
    out.flush();
  }

  private String escape(String str) {
    // longest possible escape sequence for a character is 10 chars
    int pos = 0;
    char buf[] = new char[str.length() * 10];

    for (int ix = 0; ix < str.length(); ix++) {
      char ch = str.charAt(ix);
      if (ch == 0x0020 || ch == 0x0021 ||
          (ch >= 0x0023 && ch <= 0x005B) ||
          (ch >= 0x005D && ch <= 0x007E))
        buf[pos++] = ch;
      else {
        buf[pos++] = '\\'; // all the cases below need escaping
        if (ch < 0x0008 ||
            ch == 0x000B || ch == 0x000C ||
            (ch >= 0x000E && ch <= 0x001F) ||
            (ch >= 0x007F && ch < 0xFFFF)) {
          // this doesn't handle non-BMP characters correctly. we'll deal with
          // that if they ever show up.
          buf[pos++] = 'u';
          buf[pos++] = hex(ch >> 12);
          buf[pos++] = hex((ch >> 8) & 0x000F);
          buf[pos++] = hex((ch >> 4) & 0x000F);
          buf[pos++] = hex(ch & 0x000F);
        } else if (ch == 0x0009)
          buf[pos++] = 't';
        else if (ch == 0x000A)
          buf[pos++] = 'n';
        else if (ch == 0x000D)
          buf[pos++] = 'r';
        else if (ch == 0x0022)
          buf[pos++] = '"';
        else if (ch == 0x005C)
          buf[pos++] = '\\';
      }
    }
    
    return new String(buf, 0, pos);
  }

  private char hex(int ch) {
    if (ch < 0x000A)
      return (char) ('0' + (char) ch);
    else
      return (char) ('A' + (char) (ch - 10));
  }
}