
package no.priv.garshol.duke.utils;

import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;

import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkStatus;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.InMemoryLinkDatabase;

/**
 * Utilities for dealing with link databases.
 */
public class LinkDatabaseUtils {

  /**
   * Loads a test file into an in-memory link database.
   */
  public static LinkDatabase loadTestFile(String testfile) throws IOException {
    LinkDatabase linkdb = new InMemoryLinkDatabase();
    BufferedReader reader = new BufferedReader(new FileReader(testfile));
    String line = reader.readLine();
    while (line != null) {
      int pos = line.indexOf(',');
      
      String id1 = line.substring(1, pos);
      String id2 = line.substring(pos + 1, line.length());
      if (id1.compareTo(id2) < 0) {
        String tmp = id1;
        id1 = id2;
        id2 = tmp;
      }

      linkdb.assertLink(new Link(id1, id2, LinkStatus.ASSERTED,
                                 (line.charAt(0) == '+') ?
                                 LinkKind.SAME : LinkKind.DIFFERENT));
      
      line = reader.readLine();
    }

    reader.close();
    return linkdb;
  }
  
}