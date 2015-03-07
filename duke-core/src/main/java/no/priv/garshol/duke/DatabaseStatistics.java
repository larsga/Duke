
package no.priv.garshol.duke;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.priv.garshol.duke.databases.AbstractBlockingDatabase;
import no.priv.garshol.duke.databases.KeyFunction;
import org.xml.sax.SAXException;

/**
 * Outputs statistics on the contents of the Database in order to make
 * it easier for users to judge performance.
 * @since 1.2
 */
public class DatabaseStatistics extends AbstractCmdlineTool {

  public static void main(String[] argv) throws IOException, SAXException {
    new DatabaseStatistics().run(argv);
  }

  public void run(String[] argv) throws IOException, SAXException {
    argv = init(argv, 1, 1, null);

    // right kind of database?
    if (!(database instanceof AbstractBlockingDatabase)) {
      System.out.println("ERR: Only blocking databases are supported.");
      System.exit(2);
    }

    AbstractBlockingDatabase db = (AbstractBlockingDatabase) database;
    for (KeyFunction func : db.getKeyFunctions()) {
      System.out.println("\n===== " + func);

      // count sizes
      Map<Integer, Counter> sizes = new HashMap();
      Map blocks = db.getBlocks(func);
      for (Object block : blocks.values()) {
        int size;
        if (block instanceof String[])
          size = ((String[]) block).length;
        else if (block instanceof AbstractBlockingDatabase.Block)
          size = ((AbstractBlockingDatabase.Block) block).getIds().length;
        else
          throw new DukeException("Unknown block type: " + block);

        Counter c = sizes.get(size);
        if (c == null) {
          c = new Counter(size);
          sizes.put(size, c);
        }
        c.count += 1;
      }

      // output statistics
      List<Counter> counters = new ArrayList(sizes.values());
      Collections.sort(counters);
      for (Counter c : counters)
        System.out.println("" + c.size + ": " + c.count);
    }
  }

  protected void usage() {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.DatabaseStatistics <cfgfile>");
    System.out.println("");
    System.out.println("  --reindex: Reindex all records before counting");
  }

  static class Counter implements Comparable<Counter> {
    int size;
    int count;

    public Counter(int size) {
      this.size = size;
    }

    public int compareTo(Counter c) {
      return size - c.size;
    }
  }
}
