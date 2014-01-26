
package no.priv.garshol.duke.databases;

import no.priv.garshol.duke.utils.StringUtils;

/**
 * Helper class for writing key functions.
 * @since 1.2
 */
public abstract class AbstractKeyFunction implements KeyFunction {

  public String firstLongerThan(String value, int min) {
    if (value == null)
      return "null";

    String[] tokens = StringUtils.split(value);
    for (int ix = 0; ix < tokens.length; ix++)
      if (tokens[ix].length() > min)
        return tokens[ix];
    return tokens[0];
  }

  public String lastLongerThan(String value, int min) {
    if (value == null)
      return "null";

    String[] tokens = StringUtils.split(value);
    for (int ix = tokens.length - 1; ix >= 0; ix--)
      if (tokens[ix].length() > min)
        return tokens[ix];
    return tokens[0];
  }
  
  public String allDigits(String value) {
    if (value == null)
      return "null";
    
    char[] tmp = new char[value.length()];
    int free = 0;
    for (int ix = 0; ix < value.length(); ix++) {
      char ch = value.charAt(ix);
      if (ch >= '0' && ch <= '9')
        tmp[free++] = ch;
    }
    return new String(tmp, 0, free);
  }
  
}