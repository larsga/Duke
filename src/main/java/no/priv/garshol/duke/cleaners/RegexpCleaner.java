
package no.priv.garshol.duke.cleaners;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import no.priv.garshol.duke.Cleaner;

/**
 * Cleaner which returns the part of the input string matched by
 * either the entire regular expression or a group in the regexp.
 * The default is for it to return the contents of group number 1.
 */ 
public class RegexpCleaner implements Cleaner {
  private Pattern regexp;
  private int groupno;

  public RegexpCleaner() {
    this.groupno = 1; // default
  }
  
  public String clean(String value) {
    if (value == null || value.length() == 0)
      return null;

    Matcher matcher = regexp.matcher(value);
    if (!matcher.find())
      return null;
    return matcher.group(groupno);
  }

  public void setRegexp(String regexp) {
    this.regexp = Pattern.compile(regexp);
  }

  public void setGroup(int groupno) {
    this.groupno = groupno;
  }
}