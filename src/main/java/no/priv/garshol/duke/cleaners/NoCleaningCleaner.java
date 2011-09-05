
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;

/**
 * A cleaner which simply returns the input string.
 */
public class NoCleaningCleaner implements Cleaner {

  public String clean(String value) {
    return value;
  }
  
}