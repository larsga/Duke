
package no.priv.garshol.duke.utils;

import java.io.Reader;
import java.io.IOException;
import no.priv.garshol.duke.DukeException;

public class CSVReader {
  private Reader in;
  private char[] buf;
  private int pos; // where we are in the buffer
  private int len;
  private String[] tmp;
  private char separator;
  private String file; // for error messages, can be null

  public CSVReader(Reader in) throws IOException {
    this(in, 65386, null);
  }

  public CSVReader(Reader in, int buflen, String file) throws IOException {
    this.buf = new char[buflen];
    this.pos = 0;
    this.len = in.read(buf, 0, buf.length);
    this.tmp = new String[1000];
    this.in = in;
    this.separator = ','; // default
    this.file = file;
  }

  public void setSeparator(char separator) {
    this.separator = separator;
  }

  public String[] next() throws IOException {
    if (len == -1 || pos >= len)
      return null;

    int colno = 0;
    int rowstart = pos; // used for rebuffering at end
    int prev = pos - 1;
    boolean escaped_quote = false; // did we find an escaped quote?
    boolean startquote = false;
    while (pos < len) {
      startquote = false;
      if (buf[pos] == '"') {
        startquote = true;
        prev++;
        pos++;
      }

      // scan forward, looking for end of string
      while (true) {
        while (pos < len &&
               (startquote || buf[pos] != separator) &&
               (startquote || (buf[pos] != '\n' && buf[pos] != '\r')) &&
               !(startquote && buf[pos] == '"'))
          pos++;

        if (pos + 1 >= len ||
            (!(buf[pos] == '"' && buf[pos+1] == '"')))
          break; // we found the end of this value, so stop
        else {
          // found a "". carry on
          escaped_quote = true;
          pos += 2; // step to character after next
        }
      }

      if (escaped_quote)
        tmp[colno++] = unescape(new String(buf, prev + 1, pos - prev - 1));
      else
        tmp[colno++] = new String(buf, prev + 1, pos - prev - 1);

      if (startquote)
        pos++; // step over the '"'
      prev = pos;

      if (pos >= len)
        break; // jump out of the loop to rebuffer and try again

      if (buf[pos] == '\r' || buf[pos] == '\n') {
        pos++; // step over the \r or \n
        if (pos >= len)
          break; // jump out of the loop to rebuffer and try again
        if (buf[pos] == '\n')
          pos++; // step over this, too
        break; // we're done
      }
      pos++; // step over either separator or \n
    }

    if (pos >= len) {
      // this means we've exhausted the buffer. that again means either we've
      // read the entire stream, or we need to fill up the buffer.
      if (rowstart == 0 && len == buf.length)
        throw new DukeException("Row length bigger than buffer size (" +
                                buf.length + "); unbalanced quotes? in " +
                                file);

      System.arraycopy(buf, rowstart, buf, 0, len - rowstart);

      len = len - rowstart;
      int read = in.read(buf, len, buf.length - len);

      if (read != -1) {
        len += read;
        pos = 0;
        return next();
      } else {
        len = -1;
        if (startquote) {
          // did we ever see the corresponding end quote?
          if ((buf[pos - 1] != '"') &&
              (buf[pos - 1] != '\n' && buf[pos - 2] != '"'))
            throw new DukeException("Unbalanced quote in CSV file: " + file);
        }
      }
    }

    String[] row = new String[colno];
    for (int ix = 0; ix < colno; ix++)
      row[ix] = tmp[ix];
    return row;
  }

  public void close() throws IOException {
    in.close();
  }

  private String unescape(String val) {
    return val.replace("\"\"", "\"");
  }
}
