
package no.priv.garshol.duke.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.LinkDatabase;
import org.junit.Test;

import static org.junit.Assert.fail;

public class LinkDatabaseUtilsTest {
  private LinkDatabase db;
  
  @Test
  public void testOldStyle() throws IOException {
    // tries to load a pre-1.2 format test file
    try {
      load("old-format.txt");
      fail("accepted old-style test file");
    } catch (DukeException e) {
      // this is expected
    }
  }
  
  private void load(String filename) throws IOException {
    ClassLoader cloader = Thread.currentThread().getContextClassLoader();
    InputStream istream = cloader.getResourceAsStream(filename);
    db = LinkDatabaseUtils.loadTestFile(new InputStreamReader(istream));
  }
}