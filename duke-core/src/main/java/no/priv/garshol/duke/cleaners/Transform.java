
package no.priv.garshol.duke.cleaners;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Helper class used by AbstractRuleBasedCleaner.
 */
public class Transform {
  private Pattern regex;
  private String replacement;
  private int groupno;

  public Transform(String regex, String replacement) {
    this(regex, replacement, 1);
  }
  
  public Transform(String regex, String replacement, int groupno) {
    this.regex = Pattern.compile(regex);
    this.replacement = replacement;
    this.groupno = groupno;
  }
    
  public String transform(String value) {
    Matcher m = regex.matcher(value);
    if (!m.find())
      return value;
    
    return value.substring(0, m.start(groupno)) +
           replacement +
           value.substring(m.end(groupno), value.length());
  }
}
