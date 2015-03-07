
package no.priv.garshol.duke;

import java.util.Collection;
import java.util.Collections;
import java.io.IOException;

import org.xml.sax.SAXException;

import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.utils.CommandLineParser;
import no.priv.garshol.duke.matchers.PrintMatchListener;

/**
 * Search for records and display the matching ones.
 */
public class RecordSearch extends AbstractCmdlineTool {

  public static void main(String[] argv) throws IOException, SAXException {
    new RecordSearch().run(argv);
  }

  public void run(String[] argv)
    throws IOException, SAXException {
    Collection<CommandLineParser.Option> options =
      Collections.singleton((CommandLineParser.Option) new CommandLineParser.StringOption("maxhits", 'H'));
    argv = init(argv, 3, 3, options);
    int max_hits = 10000;
    if (parser.getOptionValue("maxhits") != null)
      max_hits = Integer.parseInt(parser.getOptionValue("maxhits"));

    // build record
    RecordImpl prototype = new RecordImpl();
    prototype.addValue(argv[1], argv[2]);

    // search
    Collection<Record> records = database.findCandidateMatches(prototype);
    int hitno = 1;
    for (Record record : records) {
      PrintMatchListener.prettyPrint(record, config.getProperties());
      System.out.println();
      if (hitno++ == max_hits)
        break;
    }
  }

  protected void usage() {
    System.out.println("");
    System.out.println("java no.priv.garshol.duke.RecordSearch <cfgfile> <property> <query>");
    System.out.println("");
    System.out.println("  --reindex: Reindex all records before comparing");
    System.out.println("  --maxhits: Don't return more than this number of records");
  }
}
