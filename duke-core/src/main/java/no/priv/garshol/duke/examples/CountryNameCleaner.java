
package no.priv.garshol.duke.examples;

import no.priv.garshol.duke.Cleaner;
import no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner;

public class CountryNameCleaner implements Cleaner {
  private LowerCaseNormalizeCleaner sub;

  public CountryNameCleaner() {
    this.sub = new LowerCaseNormalizeCleaner();
  }

  public String clean(String value) {
    // do basic cleaning
    value = sub.clean(value);
    if (value == null || value.equals(""))
      return "";

    // do our stuff
    if (value.startsWith("the "))
      value = value.substring(4);

    return value;
  }  
}