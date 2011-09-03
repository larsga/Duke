
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.utils.StringUtils;

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

    if (v1.length() + v2.length() > 20 && Levenshtein.distance(v1, v2) == 1)
      return 0.95;
    
    String[] t1 = StringUtils.split(v1);
    String[] t2 = StringUtils.split(v2);

    // t1 must always be the longest
    if (t1.length < t2.length) {
      String[] tmp = t2;
      t2 = t1;
      t1 = tmp;
    }    

    // penalty imposed by pre-processing
    double penalty = 0;
    
    // if the two are of unequal lengths, make some simple checks
    if (t1.length != t2.length && t2.length >= 2) {
      // is the first token in t1 an initial? if so, get rid of it
      if ((t1[0].length() == 2 && t1[0].charAt(1) == '.') ||
          t1[0].length() == 1) {
        String[] tmp = new String[t1.length - 1];
        for (int ix = 1; ix < t1.length; ix++)
          tmp[ix - 1] = t1[ix];
        t1 = tmp;
        penalty = 0.2; // impose a penalty
      } else {
        // use similarity between first and last tokens, ignoring what's
        // in between
        int d1 = Levenshtein.distance(t1[0], t2[0]);
        int d2 = Levenshtein.distance(t1[t1.length - 1], t2[t2.length - 1]);
        return (0.4 / (d1 + 1)) + (0.4 / (d2 + 1));
      }
    }

    // if the two are of the same length, go through and compare one by one
    if (t1.length == t2.length) {
      // are the names just reversed?
      if (t1.length == 2 && t1[0].equals(t2[1]) && t1[1].equals(t2[0]))
        return 0.9;

      // normal one-by-one comparison
      double points = 1.0 - penalty;
      for (int ix = 0; ix < t1.length && points > 0; ix++) {
        int d = Levenshtein.distance(t1[ix], t2[ix]);
        
        if (ix == 0 && d > 0 &&
            (t1[ix].startsWith(t2[ix]) || t2[ix].startsWith(t1[ix]))) {
          // are we at the first name, and one is a prefix of the other?
          // if so, we treat this as edit distance 1
          d = 1;
        } else if (d > 1 && (ix + 1) <= t1.length) {
          // is it an initial? ie: "marius" ~ "m." or "marius" ~ "m"
          String s1 = t1[ix];
          String s2 = t2[ix];
          // ensure s1 is the longer
          if (s1.length() < s2.length()) {
            String tmp = s1;
            s1 = s2;
            s2 = tmp;
          }
          if ((s2.length() == 2 && s2.charAt(1) == '.') ||
              s2.length() == 1) {
            // so, s2 is an initial
            if (s2.charAt(0) == s1.charAt(0))
              d = 1; // initial matches token in other name
          }
        } else if (t1[ix].length() + t2[ix].length() <= 4)
          // it's not an initial, so if the strings are 4 characters
          // or less, we quadruple the edit dist
          d = d * 4;
        else if (t1[ix].length() + t2[ix].length() <= 6)
          // it's not an initial, so if the strings are 3 characters
          // or less, we triple the edit dist
          d = d * 3;
        else if (t1[ix].length() + t2[ix].length() <= 8)
          // it's not an initial, so if the strings are 4 characters
          // or less, we double the edit dist
          d = d * 2;
        
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