
package no.priv.garshol.duke.examples;

import no.priv.garshol.duke.Cleaner;
import no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner;

public class CapitalCleaner implements Cleaner {
  private LowerCaseNormalizeCleaner sub;

  public CapitalCleaner() {
    this.sub = new LowerCaseNormalizeCleaner();
  }

  public String clean(String value) {
    // do basic cleaning
    value = sub.clean(value);
    if (value == null || value.equals(""))
      return "";

    // do our stuff
    int ix = value.indexOf(',');
    if (ix != -1)
      value = value.substring(0, ix);

    return value;
  }  
}