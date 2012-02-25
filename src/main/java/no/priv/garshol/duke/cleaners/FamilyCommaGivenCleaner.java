
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;

/**
 * <b>Experimental</b> cleaner for person names of the form "Smith, John".
 * Basedon the PersonNameCleaner.
 */
public class FamilyCommaGivenCleaner implements Cleaner {
  private PersonNameCleaner sub;

  public FamilyCommaGivenCleaner() {
    this.sub = new PersonNameCleaner();
  }

  public String clean(String value) {
    int ix = value.indexOf(',');

    if (ix != -1)
      value = value.substring(ix + 1) + " " + value.substring(0, ix);

    return sub.clean(value);
  }
}