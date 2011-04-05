
package no.priv.garshol.duke.test;

import org.junit.Test;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import no.priv.garshol.duke.CSVReader;

public class CSVReaderTest {

  @Test
  public void testEmpty() throws IOException {
    String data = "";
    CSVReader reader = new CSVReader(new StringReader(data));
    assertTrue("couldn't read empty file correctly", reader.next() == null);
  }

  @Test
  public void testOneRow() throws IOException {
    String data = "a,b,c";
    CSVReader reader = new CSVReader(new StringReader(data));

    String[] row = reader.next();
    assertEquals("first row read incorrectly", new String[]{"a", "b", "c"},
                 null);
    assertTrue("reading not terminated correctly", reader.next() == null);
 }
  
}