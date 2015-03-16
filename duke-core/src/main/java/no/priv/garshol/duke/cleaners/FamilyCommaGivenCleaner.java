
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;

/**
 * <b>Experimental</b> cleaner for person names of the form "Smith,
 * John".  Based on the PersonNameCleaner. It also normalizes periods
 * in initials, so that "J.R. Ackerley" becomes "J. R. Ackerley".
 */
public class FamilyCommaGivenCleaner implements Cleaner {
  private PersonNameCleaner sub;

  public FamilyCommaGivenCleaner() {
    this.sub = new PersonNameCleaner();
  }

  public String clean(String value) {
    int i = value.indexOf(',');
    if (i != -1)
      value = value.substring(i + 1) + " " + value.substring(0, i);

    char[] tmp = new char[value.length() * 2];
    int pos = 0;
    for (int ix = 0; ix < value.length(); ix++) {
      tmp[pos++] = value.charAt(ix);
      if (value.charAt(ix) == '.' &&
          ix+1 < value.length() &&
          value.charAt(ix + 1) != ' ')
        tmp[pos++] = ' ';
    }

    return sub.clean(new String(tmp, 0, pos));
  }
}