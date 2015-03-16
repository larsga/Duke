
package no.priv.garshol.duke.utils;

import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;

import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkStatus;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.DukeException;
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
   * @since 1.2
   */
  public static LinkDatabase loadTestFile(Reader reader) throws IOException {
    LinkDatabase linkdb = new InMemoryLinkDatabase();
    loadTestFile(reader, linkdb);
    return linkdb;
  }
  
  /**
   * Loads a test file into an in-memory link database.
   */
  public static void loadTestFile(String testfile, LinkDatabase linkdb)
    throws IOException {
    loadTestFile(new FileReader(testfile), linkdb);
  }

  /**
   * Loads a test file into an in-memory link database.
   * @since 1.2
   */
  public static void loadTestFile(Reader input, LinkDatabase linkdb)
    throws IOException {
    CSVReader reader = new CSVReader(input);
    String[] row = reader.next();
    while (row != null) {
      if (row.length != 4)
        throw new DukeException("Wrong test file format, row had " +
                                row.length + " values, should be 4");
        
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