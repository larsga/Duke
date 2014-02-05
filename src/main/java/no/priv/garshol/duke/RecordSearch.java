
package no.priv.garshol.duke;

import java.util.Collection;
import java.io.IOException;

import org.xml.sax.SAXException;

import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.matchers.PrintMatchListener;

/**
 * Search for records and display the matching ones.
 */
public class RecordSearch extends AbstractCmdlineTool {
  private static final int MAX_SEARCH_HITS = 10;

  public static void main(String[] argv) throws IOException, SAXException {
    new RecordSearch().run(argv);
  }

  public void run(String[] argv)
    throws IOException, SAXException {
    argv = init(argv, 3, 3);
    
    // build record
    RecordImpl prototype = new RecordImpl();
    prototype.addValue(argv[1], argv[2]);
    
    // search
    Collection<Record> records = database.findCandidateMatches(prototype);
    for (Record record : records) {
      PrintMatchListener.prettyPrint(record, config.getProperties());
      System.out.println();
    }
  }

  protected void usage() {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.RecordSearch <cfgfile> <property> <query>");
    System.out.println("");
    System.out.println("  --reindex: Reindex all records before comparing");
  }  
}
