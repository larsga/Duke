
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;

/**
 * A cleaner which removes leading and trailing whitespace, without
 * making any other changes.
 */
public class TrimCleaner implements Cleaner {

  public String clean(String value) {
    value = value.trim();
    if (value.equals(""))
      return null;
    return value;
  }
  
}