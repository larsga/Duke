
package no.priv.garshol.duke.examples;

import no.priv.garshol.duke.Cleaner;

public class ExtractYearCleaner implements Cleaner {

  public String clean(String value) {
    try {
      int year = Integer.parseInt(value.substring(0, 4));
      if (year == 1)
        return null;
      return "" + year;
    } catch (Exception e) {
      return null;
    }
  }  
}