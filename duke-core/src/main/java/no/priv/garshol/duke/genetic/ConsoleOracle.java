
package no.priv.garshol.duke.genetic;

import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;

import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.utils.YesNoConsole;
import no.priv.garshol.duke.utils.LinkFileWriter;

/**
 * This oracle asks the user via the console.
 */
public class ConsoleOracle implements Oracle {
  private YesNoConsole console;
  private LinkFileWriter writer;
  private Writer out;

  public ConsoleOracle() {
    this.console = new YesNoConsole();
  }
  
  public LinkKind getLinkKind(String id1, String id2) {
    boolean match = console.yesorno();
    if (writer != null)
      try {
        writer.write(id1, id2, match, 1.0);
        out.flush(); // make sure everything's saved
      } catch (IOException e) {
        throw new DukeException(e);
      }
    return match ? LinkKind.SAME : LinkKind.DIFFERENT;
  }

  public void setLinkFile(String linkfile) throws IOException {
    out = new FileWriter(linkfile, true);
    writer = new LinkFileWriter(out);
    // FIXME: strictly speaking, this leaks file handles. in practice it
    // probably won't matter
  }
}