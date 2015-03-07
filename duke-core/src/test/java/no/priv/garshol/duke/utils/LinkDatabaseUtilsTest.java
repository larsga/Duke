
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Rule;
import org.junit.After;
import org.junit.Before;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkStatus;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.utils.LinkFileWriter;
import no.priv.garshol.duke.utils.LinkDatabaseUtils;

import static no.priv.garshol.duke.test.LinkDatabaseMatchListenerTest.verifySame;

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