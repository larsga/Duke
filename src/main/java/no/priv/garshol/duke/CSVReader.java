
package no.priv.garshol.duke;

import java.io.Reader;
import java.io.IOException;

// FIXME: buffer handling
//   probably fix by finishing up, and at the end of the method we detect
//   that it's the end of the buffer. try to get more, and if that's not
//   possible we wind up. if it is possible we reshuffle buffer and try
//   again

// FIXME: \r

public class CSVReader {
  private Reader in;
  private char[] buf;
  private int pos; // where we are in the buffer
  private int len;
  private String[] tmp;

  public CSVReader(Reader in) throws IOException {
    this.buf = new char[65386];
    this.pos = 0;
    this.len = in.read(buf, 0, buf.length);
    this.tmp = new String[1000];
    this.in = in;
  }

  public String[] next() throws IOException {
    if (len == -1 || pos >= len)
      return null;

    int colno = 0;
    int prev = pos - 1;
    while (pos < len) {
      boolean startquote = false;
      if (buf[pos] == '"') {
        startquote = true;
        prev++;
        pos++;
      }
      
      while (pos < len && buf[pos] != ',' &&
             (startquote || buf[pos] != '\n') &&
             !(startquote && buf[pos] == '"'))
        pos++;

      tmp[colno++] = new String(buf, prev + 1, pos - prev - 1);
      if (startquote)
        pos++; // step over the '"'
      prev = pos;
      
      if (buf[pos++] == '\n') // ++ steps over separator
        break; // we're done
    }

    String[] row = new String[colno];
    for (int ix = 0; ix < colno; ix++)
      row[ix] = tmp[ix];
    return row;
  }

  public void close() throws IOException {
    in.close();
  }
  
}