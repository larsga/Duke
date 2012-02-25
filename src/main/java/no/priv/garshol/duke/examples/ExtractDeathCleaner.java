
package no.priv.garshol.duke.examples;

import no.priv.garshol.duke.Cleaner;

public class ExtractDeathCleaner implements Cleaner {

  public String clean(String value) {
    int pos = value.indexOf('-');
    if (pos == -1)
      return null;

    return value.substring(pos + 1);
  }  
}