
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
    loadTestFile(testfile, linkdb);
    return linkdb;
  }

  /**
   * Loads a test file into an in-memory link database.
   */
  public static void loadTestFile(String testfile, LinkDatabase linkdb)
    throws IOException {
    CSVReader reader = new CSVReader(new FileReader(testfile));
    String[] row = reader.next();
    while (row != null) {
      LinkKind kind = row[0].equals("+") ? LinkKind.SAME : LinkKind.DIFFERENT;
      String id1 = row[1];
      String id2 = row[2];
      if (id1.compareTo(id2) < 0) {
        String tmp = id1;
        id1 = id2;
        id2 = tmp;
      }
      double conf = Double.valueOf(row[3]);

      linkdb.assertLink(new Link(id1, id2, LinkStatus.ASSERTED, kind, conf));
      
      row = reader.next();
    }

    reader.close();
  }
  
}