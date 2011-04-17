
package no.priv.garshol.duke;

/**
 * An operator which knows about comparing names. It tokenizes, and
 * also applies Levenshtein distance.
 */
public class PersonNameComparator implements Comparator {

  public boolean isTokenized() {
    return true;
  }
  
  public double compare(String v1, String v2) {
    if (v1.equals(v2))
      return 1.0;

    if (Levenshtein.distance(v1, v2) == 1)
      return 0.9;
    
    String[] t1 = StringUtils.split(v1);
    String[] t2 = StringUtils.split(v2);

    // t1 must always be the longest
    if (t1.length < t2.length) {
      String[] tmp = t2;
      t2 = t1;
      t1 = tmp;
    }    

    // if the two are of unequal lengths, check if first and last are the same
    if (t1.length != t2.length && t2.length >= 2) {
      int d1 = Levenshtein.distance(t1[0], t2[0]);
      int d2 = Levenshtein.distance(t1[t1.length - 1], t2[t2.length - 1]);
      
      return (0.4 / (d1 + 1)) + (0.4 / (d2 + 1));
    }

    // if the two are of the same length, go through and compare one by one
    if (t1.length == t2.length) {
      // are the names just reversed?
      if (t1.length == 2 && t1[0].equals(t2[1]) && t1[1].equals(t2[0]))
        return 0.9;

      // normal one-by-one comparison
      double points = 1.0;
      for (int ix = 0; ix < t1.length && points > 0; ix++) {
        int d = Levenshtein.distance(t1[ix], t2[ix]);

        // is it an initial? ie: "marius" ~ "m." or "marius" ~ "m"
        if (d > 1 && ix > 0 && (ix + 1) <= t1.length) {
          String s1 = t1[ix];
          String s2 = t2[ix];
          // ensure s1 is the longer
          if (s1.length() < s2.length()) {
            String tmp = s1;
            s1 = s2;
            s2 = tmp;
          }
          if ((s2.length() == 2 && s2.charAt(1) == '.' && 
               s2.charAt(0) == s1.charAt(0)) ||
              (s2.length() == 1 && s2.charAt(0) == s1.charAt(0)))
            d = 1; // we treat this as an edit distance of 1
        }

        points -= d * 0.1;
      }

      // if both are just one token, be strict
      if (t1.length == 1 && points < 0.8)
        return 0.0;
      
      return Math.max(points, 0.0);
    }
    
    return 0.0;
  }
  
}