
package no.priv.garshol.duke.cleaners;

import java.util.Map;
import java.util.HashMap;
import no.priv.garshol.duke.Cleaner;

/**
 * A cleaner that removes HTML-style entity references, such as
 * &amp;#222; and &amp;mdash;.
 * @since 1.3
 */
public class HTMLCleaner implements Cleaner {
  private static Map<String, String> entities;

  static {
    entities = new HashMap();
    entities.put("mdash", "\u2014");
  }
  
  public String clean(String value) {
    StringBuilder buf = new StringBuilder(value.length());
    for (int ix = 0; ix < value.length(); ix++) {
      char ch = value.charAt(ix);
      if (ch != '&') {
        buf.append(ch);
        continue;
      }

      ch = value.charAt(++ix);
      if (ch == '#') {
        ix++;
        if (value.charAt(ix) == 'x')
          throw new UnsupportedOperationException("Don't support &#x...;");
        int pos = ix;
        for (; ix < value.length() && value.charAt(ix) != ';'; ix++)
          ;
        ch = (char) Integer.parseInt(value.substring(pos, ix));
        buf.append(ch);
      } else {
        int pos = ix;
        for (; ix < value.length() && value.charAt(ix) != ';'; ix++)
          ;
        String v = entities.get(value.substring(pos, ix));
        buf.append(v);
      }
    }
    return buf.toString();
  }
}
