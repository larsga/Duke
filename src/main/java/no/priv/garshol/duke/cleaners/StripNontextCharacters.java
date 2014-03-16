
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;

/**
 * A cleaner which removes non-text characters. Specifically it strips
 * control characters (0-0x1F, 0x7F-0x9F) and special symbols in the
 * range 0xA1-0xBF.
 */
public class StripNontextCharacters implements Cleaner {

  public String clean(String value) {
    char[] tmp = new char[value.length()];
    int pos = 0;
    for (int ix = 0; ix < value.length(); ix++) {
      char ch = value.charAt(ix);
      if (ch < 0x20 ||
          (ch >= 0x7F && ch < 0xA0) || 
          (ch > 0xA0 && ch < 0xC0))
        continue; // skip Euro symbol, soft hyphen, etc etc
      tmp[pos++] = ch;
    }
    return new String(tmp, 0, pos);
  }  
}
