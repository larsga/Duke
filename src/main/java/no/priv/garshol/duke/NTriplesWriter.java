
package no.priv.garshol.duke;

import java.io.Writer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * A simplified NTriples serializer, used for writing the link file to
 * NTriples format.
 */
public class NTriplesWriter implements StatementHandler {
  private Writer out;

  // using a stream so we can control the encoding
  public NTriplesWriter(OutputStream out) {
    try {
      this.out = new OutputStreamWriter(out, "ascii");
    } catch (java.io.UnsupportedEncodingException e) {
      // can't think of any good reason why this needs to be a checked
      // exception
      throw new RuntimeException(e);
    }
  }

  public void statement(String subject, String property, String object,
                        boolean literal) {
    try {
      out.write("<" + subject + "> ");
      out.write("<" + property + "> ");

      if (literal)
        out.write('"' + object + '"' + ' '); // FIXME: needs escaping
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
  
}