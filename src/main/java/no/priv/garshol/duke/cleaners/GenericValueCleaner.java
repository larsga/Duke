
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;

/**
 * A cleaner which returns values as they are, but removes specific
 * values. This is useful in cases where users have entered so-called
 * "generic values". For example, if the unknown company number is
 * always set as "999999999", then you can use this cleaner to remove
 * that specific value.
 */
public class GenericValueCleaner implements Cleaner {
  private String generic;
  
  public String clean(String value) {
    if (generic.equals(value))
      return null;
    return value;
  }

  public void setGeneric(String generic) {
    this.generic = generic;
  }
  
}