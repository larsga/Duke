
package no.priv.garshol.duke;

/**
 * An implementation of the Jaro-Winkler string similarity measure.
 */
public class JaroWinkler implements Comparator {

  public double compare(String s1, String s2) {
    return similarity(s1, s2);
  }

  public boolean isTokenized() {
    return true; // I guess?
  }
  
  /**
   * Returns normalized score, with 0.0 meaning no similarity at all,
   * and 1.0 meaning full equality.
   */
  public static double similarity(String s1, String s2) {
    if (s1.equals(s2))
      return 1.0;

    // ensure that s1 is shorter than or same length as s2
    if (s1.length() > s2.length()) {
      String tmp = s2;
      s2 = s1;
      s1 = tmp;
    }

    // (1) find the number of characters the two strings have in common.
    // note that matching characters can only be half the length of the
    // longer string apart.
    int maxdist = s2.length() / 2;
    int c = 0; // count of common characters
    int t = 0; // count of transpositions
    int prevpos = -1;
    for (int ix = 0; ix < s1.length(); ix++) {
      char ch = s1.charAt(ix);

      // now try to find it in s2
      for (int ix2 = Math.max(0, ix - maxdist);
           ix2 < Math.min(s2.length(), ix + maxdist);
           ix2++) {
        if (ch == s2.charAt(ix2)) {
          c++; // we found a common character
          if (prevpos != -1 && ix2 < prevpos)
            t++; // moved back before earlier 
          prevpos = ix2;
          break;
        }
      }
    }

    // we don't divide t by 2 because as far as we can tell, the above
    // code counts transpositions directly.

    // System.out.println("c: " + c);
    // System.out.println("t: " + t);
    // System.out.println("c/m: " + (c / (double) s1.length()));
    // System.out.println("c/n: " + (c / (double) s2.length()));
    // System.out.println("(c-t)/c: " + ((c - t) / (double) c));
    
    // we might have to give up right here
    if (c == 0)
      return 0.0;
    
    return ((c / (double) s1.length()) +
            (c / (double) s2.length()) +
            ((c - t) / (double) c)) / 3.0;
  }
  
}