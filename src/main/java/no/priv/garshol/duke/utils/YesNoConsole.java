
package no.priv.garshol.duke.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import no.priv.garshol.duke.DukeException;

public class YesNoConsole {
  private BufferedReader console;

  public YesNoConsole() {
    this.console = new BufferedReader(new InputStreamReader(System.in));
  }
  
  public boolean yesorno() {
    System.out.print("Correct? (Y/N) ");
    try {
      String line = console.readLine();
      if (line == null)
        throw new DukeException("End of file on console");
      line = line.trim();
      
      if (line.equalsIgnoreCase("Y"))
        return true;
      else if (line.equalsIgnoreCase("N"))
        return false;
      else
        return yesorno();
    } catch (IOException e) {
      throw new DukeException("Couldn't read input line", e);
    }
  }
}
