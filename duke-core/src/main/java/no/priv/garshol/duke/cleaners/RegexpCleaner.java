
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cleaner which returns the part of the input string matched by
 * either the entire regular expression or a group in the regexp.
 * The default is for it to return the contents of group number 1.
 *
 * Can also discard the matching group and return rest of string.
 * @since 0.5
 */ 
public class RegexpCleaner implements Cleaner {
  private Pattern regexp;
  private int groupno;
  // if true, discard group, otherwise keep only group. default: false
  private boolean discard;
  // if true, discard all result of group. default: false
  private boolean discardAllGroup;

  public RegexpCleaner() {
    this.groupno = 1; // default
  }
  
  public String clean(String value) {
    if (value == null || value.length() == 0)
      return null;

    Matcher matcher = regexp.matcher(value);
    if (!discard && !discardAllGroup) {
      if (!matcher.find())
        return null;
      return matcher.group(groupno);
    } else {
      if (!matcher.find())
        return value;
      else {
        StringBuilder discardBuilder = new StringBuilder(value);
        discardBuilder.delete(matcher.start(groupno), matcher.end(groupno));
        if (discardAllGroup) {
          Matcher discardAllMatcher = regexp.matcher(discardBuilder);
          while(discardAllMatcher.find()) {
            discardBuilder.delete(discardAllMatcher.start(groupno), discardAllMatcher.end(groupno));
            discardAllMatcher.reset();
          }
        }
        return discardBuilder.toString();
      }
    }
  }

  public void setRegexp(String regexp) {
    this.regexp = Pattern.compile(regexp);
  }

  /**
   * The group in the pattern to keep or discard
   * @param groupno
   */
  public void setGroup(int groupno) {
    this.groupno = groupno;
  }

  /**
   * If true, discards the first occurrence of matching {@code group} instead of keeping it.
   * @param discard
   */
  public void setDiscardGroup(boolean discard) {
    this.discard = discard;
  }

  /**
   * If true, discards all results of matching {@code group}
   * @param discardAllGroup
   */
  public void setDiscardAllGroup(boolean discardAllGroup) {
        this.discardAllGroup = discardAllGroup;
    }
}