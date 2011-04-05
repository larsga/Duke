
package no.priv.garshol.duke;

import java.io.Reader;
import java.io.IOException;

public class CSVReader {
  private Reader in;
  private char[] buf;
  private int pos;
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
    System.out.println("len: " + len);
    if (len == -1)
      return null;

    int colno = 0;
    int prev = -1;
    int ix = 0;
    while (ix < len) {
      while (ix < len && buf[ix] != ',' && buf[ix] != '\n')
        ix++;

      tmp[colno] = new String(buf, prev + 1, ix - prev);
      prev = ix;

      if (buf[ix] == '\n')
        break; // we're done

      ix++; // step over the ','
      colno++;
    }

    String[] row = new String[colno];
    for (ix = 0; ix < colno; ix++)
      row[ix] = tmp[ix];
    return row;
  }

  public void close() throws IOException {
    in.close();
  }
  
}