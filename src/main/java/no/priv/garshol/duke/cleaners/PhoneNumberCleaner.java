
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;

/**
 * <p>Cleaner for international phone numbers. It assumes that it can
 * get the same phone number in forms like:
 *
 * <ul>
 *   <li>+47 55301400
 *   <li>47-55-301400
 * </ul>
 */ 
public class PhoneNumberCleaner implements Cleaner {

  public String clean(String value) {
    // to begin with, let's try keeping only the digits
    char[] tmp = new char[value.length()];
    int pos = 0;
    for (int ix = 0; ix < tmp.length; ix++) {
      char ch = value.charAt(ix);
      if (ch >= '0' && ch <= '9')
        tmp[pos++] = ch;
    }
    return new String(tmp, 0, pos);
  }
  
}