
package no.priv.garshol.duke.comparators;

/**
 * Wrapping this around the input string to simplify the code.
 */
public class Matcher {
  private String str;
  private int ix;

  public Matcher(String str) {
    this.str = str;
    this.ix = -1;
  }

  public boolean isNext(char ch) {
    return ix + 1 < str.length() && str.charAt(ix + 1) == ch;
  }

  public boolean atStart() {
    return ix == 0;
  }

  public boolean hasNext() {
    return ix + 1 < str.length();
  }

  public boolean nextIsLast() {
    return ix + 2 == str.length();
  }

  public boolean isLast() {
    return ix + 1 == str.length();
  }
    
  public char next() {
    return str.charAt(++ix);
  }

  public void skip() {
    ix++;
  }

  public boolean previousOneOf(String chars) {
    if (ix == 0)
      return false;
    return chars.indexOf(str.charAt(ix - 1)) != -1;
  }
}
