
package no.priv.garshol.duke.cleaners;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import no.priv.garshol.duke.Cleaner;

/**
 * Cleaner which returns the part of the input string matched by
 * either the entire regular expression or a group in the regexp.
 * The default is for it to return the contents of group number 1.
 * @since 0.5
 */ 
public class RegexpCleaner implements Cleaner {
  private Pattern regexp;
  private int groupno;
  private Cleaner sub;
  // if true, discard group, otherwise keep only group. default: false
  private boolean discard; 

  public RegexpCleaner() {
    this.groupno = 1; // default
  }
  
  public String clean(String value) {
    if (sub != null)
      value = sub.clean(value);
    
    if (value == null || value.length() == 0)
      return null;

    Matcher matcher = regexp.matcher(value);
    if (!discard) {
      if (!matcher.find())
        return null;
      return matcher.group(groupno);
    } else {
      if (!matcher.find())
        return value;

      return value.substring(0, matcher.start(groupno)) +
             value.substring(matcher.end(groupno));
    }
  }

  public void setRegexp(String regexp) {
    this.regexp = Pattern.compile(regexp);
  }

  public void setGroup(int groupno) {
    this.groupno = groupno;
  }

  public void setSubcleaner(Cleaner sub) {
    this.sub = sub;
  }

  public void setDiscardGroup(boolean discard) {
    this.discard = discard;
  }
}