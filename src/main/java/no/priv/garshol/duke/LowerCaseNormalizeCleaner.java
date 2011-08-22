
package no.priv.garshol.duke;

import java.text.Normalizer;

public class LowerCaseNormalizeCleaner implements Cleaner {
  private boolean strip_accents = true;

  public void setStripAccents(boolean strip_accents) {
    this.strip_accents = strip_accents;
  }
  
  public String clean(String value) {
    if (strip_accents) 
      // after this, accents will be represented as separate combining
      // accent characters trailing the character they belong with. the
      // next step will strip them out.
      value = Normalizer.normalize(value, Normalizer.Form.NFD);
    
    char[] tmp = new char[value.length()];
    int pos = 0;
    boolean prevws = false;
    for (int ix = 0; ix < tmp.length; ix++) {
      char ch = value.charAt(ix);

      // if character is combining diacritical mark, skip it
      if ((ch >= 0x0300 && ch <= 0x036F) ||
          (ch >= 0x1DC0 && ch <= 0x1DFF) ||
          (ch >= 0x20D0 && ch <= 0x20FF) ||
          (ch >= 0xFE20 && ch <= 0xFE2F))
        continue;

      // whitespace processing
      if (ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r') {
        if (prevws && pos != 0)
          tmp[pos++] = ' ';

        tmp[pos++] = Character.toLowerCase(ch);
        
        prevws = false;
      } else
        prevws = true;
    }
    return new String(tmp, 0, pos);
  }
}