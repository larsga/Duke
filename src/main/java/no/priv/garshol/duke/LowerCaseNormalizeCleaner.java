
package no.priv.garshol.duke;

public class LowerCaseNormalizeCleaner implements Cleaner {

  public String clean(String value) {
    char[] tmp = new char[value.length()];
    int pos = 0;
    boolean prevws = false;
    for (int ix = 0; ix < tmp.length; ix++) {
      char ch = value.charAt(ix);
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