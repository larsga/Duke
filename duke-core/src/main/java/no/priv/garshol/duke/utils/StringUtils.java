
package no.priv.garshol.duke.utils;

public class StringUtils {

  /**
   * Replaces all characters in the second parameter found in the first
   * parameter with the final character.
   * @param value the string to replace characters in
   * @param chars the characters to replace
   * @param replacement the character to insert as replacement
   */
  public static String replaceAnyOf(String value, String chars,
                                    char replacement) {
    char[] tmp = new char[value.length()];
    int pos = 0;
    for (int ix = 0; ix < tmp.length; ix++) {
      char ch = value.charAt(ix);
      if (chars.indexOf(ch) != -1)
        tmp[pos++] = replacement;
      else
        tmp[pos++] = ch;
    }
    return new String(tmp, 0, tmp.length);
  }

  /**
   * Removes trailing and leading whitespace, and also reduces each
   * sequence of internal whitespace to a single space.
   */
  public static String normalizeWS(String value) {
    char[] tmp = new char[value.length()];
    int pos = 0;
    boolean prevws = false;
    for (int ix = 0; ix < tmp.length; ix++) {
      char ch = value.charAt(ix);
      if (ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r') {
        if (prevws && pos != 0)
          tmp[pos++] = ' ';

        tmp[pos++] = ch;
        prevws = false;
      } else
        prevws = true;
    }
    return new String(tmp, 0, pos);
  }
  
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

  public static String join(String[] pieces) {
    StringBuilder tmp = new StringBuilder();
    for (int ix = 0; ix < pieces.length; ix++) {
      if (ix != 0)
        tmp.append(" ");
      tmp.append(pieces[ix]);
    }
    return tmp.toString();
  }
}