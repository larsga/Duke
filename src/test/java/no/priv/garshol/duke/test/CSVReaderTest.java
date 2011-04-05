
package no.priv.garshol.duke.test;

import org.junit.Test;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import junit.framework.AssertionFailedError;

import java.io.IOException;
import java.io.StringReader;
import no.priv.garshol.duke.CSVReader;

public class CSVReaderTest {

  @Test
  public void testEmpty() throws IOException {
    String data = "";
    CSVReader reader = new CSVReader(new StringReader(data));
    compareRows("couldn't read empty file correctly", null, reader.next());
  }

  @Test
  public void testOneRow() throws IOException {
    String data = "a,b,c";
    CSVReader reader = new CSVReader(new StringReader(data));

    String[] row = reader.next();
    compareRows("first row read incorrectly", new String[]{"a", "b", "c"},
                row);
    compareRows("reading not terminated correctly", null, reader.next());
  }

  @Test
  public void testTwoRows() throws IOException {
    String data = "a,b,c\nd,e,f";
    CSVReader reader = new CSVReader(new StringReader(data));

    String[] row = reader.next();
    compareRows("first row read incorrectly", new String[]{"a", "b", "c"},
                row);
    row = reader.next();
    compareRows("second row read incorrectly", new String[]{"d", "e", "f"},
                row);
    compareRows("reading not terminated correctly", null, reader.next());
  }

  @Test
  public void testOneRowQuotes() throws IOException {
    String data = "\"a\",\"b\",\"c\"";
    CSVReader reader = new CSVReader(new StringReader(data));

    String[] row = reader.next();
    compareRows("first row read incorrectly", new String[]{"a", "b", "c"},
                row);
    compareRows("reading not terminated correctly", null, reader.next());
  }

  @Test
  public void testOneRowInconsistentQuotes() throws IOException {
    String data = "\"a\",b,\"c\"";
    CSVReader reader = new CSVReader(new StringReader(data));

    String[] row = reader.next();
    compareRows("first row read incorrectly", new String[]{"a", "b", "c"},
                row);
    compareRows("reading not terminated correctly", null, reader.next());
  }

  @Test
  public void testTwoRowsQuotes() throws IOException {
    String data = "\"a\",\"b\",\"c\"\n\"d\",\"e\",\"f\"";
    CSVReader reader = new CSVReader(new StringReader(data));

    String[] row = reader.next();
    compareRows("first row read incorrectly", new String[]{"a", "b", "c"},
                row);
    row = reader.next();
    compareRows("second row read incorrectly", new String[]{"d", "e", "f"},
                row);
    compareRows("reading not terminated correctly", null, reader.next());
  }

  @Test
  public void testOneRowQuotesLineBreak() throws IOException {
    String data = "\"a\",\"b\nc\",\"d\"";
    CSVReader reader = new CSVReader(new StringReader(data));

    String[] row = reader.next();
    compareRows("first row read incorrectly", new String[]{"a", "b\nc", "d"},
                row);
    compareRows("reading not terminated correctly", null, reader.next());
  }
  
  private void compareRows(String msg, String[] row1, String[] row2) {
    if (row1 == row2)
      return;
    
    if (row1 == null && row2 != null)
      throw new AssertionFailedError(msg + " expected null, but received " +
                                     toString(row2));
    if (row1 != null && row2 == null)
      throw new AssertionFailedError(msg + " expected " + toString(row1) +
                                     ", but received null");

    boolean equals = row1.length == row2.length;
    for (int ix = 0; equals && ix < row1.length; ix++)
      if (!row1[ix].equals(row2[ix]))
        equals = false;

    if (!equals)
      throw new AssertionFailedError(msg + " expected " + toString(row1) +
                                     ", but received " + toString(row2));
  }

  public String toString(String[] row) {
    if (row == null)
      return "null";

    StringBuffer buf = new StringBuffer();
    buf.append("[");
    for (int ix = 0; ix < row.length; ix++) {
      buf.append("\"" + row[ix] + "\"");
      if (ix + 1 < row.length)
        buf.append(", ");
    }
    buf.append("]");
    return buf.toString();
  }
}