
package no.priv.garshol.duke.genetic;

import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.utils.YesNoConsole;

/**
 * This oracle asks the user via the console.
 */
public class ConsoleOracle implements Oracle {
  private YesNoConsole console;

  public ConsoleOracle() {
    this.console = new YesNoConsole();
  }
  
  public LinkKind getLinkKind(String id1, String id2) {
    if (console.yesorno())
      return LinkKind.SAME;
    else
      return LinkKind.DIFFERENT;
  }
}