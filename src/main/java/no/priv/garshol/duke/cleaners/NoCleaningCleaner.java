
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;

public class NoCleaningCleaner implements Cleaner {

  public String clean(String value) {
    return value;
  }
  
}