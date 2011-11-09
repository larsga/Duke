
package no.priv.garshol.duke.utils;

import java.util.Map;
import java.util.HashMap;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;

import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkStatus;

/**
 * The concept of a special syntax for link files is a bit dubious.
 * Should it really be normal CSV instead?
 */
public class TestFileUtils {
  public Map<String, Link> load(String testfile) throws IOException {
    Map<String, Link> links = new HashMap();
    BufferedReader reader = new BufferedReader(new FileReader(testfile));
    String line = reader.readLine();
    while (line != null) {
      int pos = line.indexOf(',');

      String id1 = line.substring(1, pos);
      String id2 = line.substring(pos + 1, line.length());

      links.put(id1 + "," + id2,
                new Link(id1, id2, LinkStatus.ASSERTED,
                         line.charAt(0) == '+' ?
                         LinkKind.SAME : LinkKind.DIFFERENT));
        
      line = reader.readLine();
    }
    reader.close();

    return links;
  }
}