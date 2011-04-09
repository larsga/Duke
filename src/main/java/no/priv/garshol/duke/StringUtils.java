
package no.priv.garshol.duke;

public class StringUtils {

  public static String[] split(String str) {
    String[] tokens = new String[(int) (str.length() / 2) + 1];
    int start = 0;
    int tcount = 0;
    boolean prevws = false;
    int ix;
    for (ix = 0; ix < str.length(); ix++) {
      if (str.charAt(ix) == ' ') {
        if (!prevws && ix > 0)
          tokens[tcount++] = str.substring(start, ix);
        prevws = true;
        start = ix + 1;
      } else
        prevws = false;
    }

    if (!prevws && start != ix)
      tokens[tcount++] = str.substring(start);

    String[] tmp = new String[tcount];
    for (ix = 0; ix < tcount; ix++)
      tmp[ix] = tokens[ix];
    return tmp;
  }
}