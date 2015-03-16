
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;

/**
 * Cleaner which removes all characters except the digits 0-9.
 */ 
public class DigitsOnlyCleaner implements Cleaner {

  public String clean(String value) {
    char[] tmp = new char[value.length()];
    int pos = 0;
    for (int ix = 0; ix < tmp.length; ix++) {
      char ch = value.charAt(ix);
      if (ch >= '0' && ch <= '9')
        tmp[pos++] = ch;
    }
    if (pos == 0)
      return null;
    return new String(tmp, 0, pos);
  }
  
}