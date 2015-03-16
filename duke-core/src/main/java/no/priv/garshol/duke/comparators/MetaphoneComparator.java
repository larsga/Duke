
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;

// http://www.wbrogden.com/java/Phonetic/index.html
// http://www.wbrogden.com/phonetic/index.html

/**
 * An implementation of the Metaphone algorithm, and a comparator
 * which considers strings to have a score of 0.9 if their Metaphone
 * values match.
 */
public class MetaphoneComparator implements Comparator {
  
  public double compare(String s1, String s2) {
    if (s1.equals(s2))
      return 1.0;

    if (metaphone(s1).equals(metaphone(s2)))
      return 0.9;

    return 0.0;
  }

  public boolean isTokenized() {
    return true; // I guess?
  }
  
  /**
   * Produces the Metaphone key for the given string.
   */
  public static String metaphone(String str) {
    if (str.length() < 1)
      return ""; // no metaphone key for the empty string

    str = str.toUpperCase();
    char[] key = new char[str.length() * 2]; // could be all X-es
    int pos = 0;

    for (int ix = 0; ix < str.length(); ix++) {
      char ch = str.charAt(ix);

      if (isVowel(ch) && ch != 'Y') {
        if (ix != 0)
          ch = ' '; // meaning: skip
        // Initial  ae-      -> drop first letter
        else if (ix == 0 && ch == 'A' && str.length() > 1 &&
                 str.charAt(ix + 1) == 'E') {
          ch = 'E';
          ix++;
        }
          
      } else {
        // skip double consonant
        if (ch != 'C' && ix + 1 < str.length() && str.charAt(ix + 1) == ch)
          ch = str.charAt(++ix);
       
        switch(ch) {
        case 'B':
          // B -> B   unless at the end of a word after "m" as in "dumb"
          if (ix + 1 == str.length() && ix != 0 &&
              str.charAt(ix - 1) == 'M')
            ch = ' '; // skip
          break;
          
        case 'C':
          // C -> X   (sh) if -cia- or -ch-
          //      S   if -ci-, -ce- or -cy-
          //      K   otherwise, including -sch-

          ch = 'K'; // default
          if (ix > 0 && str.charAt(ix - 1) == 'S' &&
              ix + 1 < str.length() && str.charAt(ix + 1) == 'H')
            ix++; // skip the 'H'
          else if (ix + 1 < str.length()) {
            char next = str.charAt(ix + 1);
            if (next == 'I' && ix + 2 < str.length() &&
                str.charAt(ix + 2) == 'A')
              ch = 'X';
            else if (next == 'I' || next == 'E' || next == 'Y')
              ch = 'S';
            else if (next == 'H') {
              ch = 'X';
              ix++; // we need to skip the H
            }
          }
          break;

        case 'D':
          // D -> J   if in -dge-, -dgy- or -dgi-
          //      T   otherwise

          if (ix + 2 < str.length() &&
              str.charAt(ix + 1) == 'G' &&
              (str.charAt(ix + 2) == 'E' ||
               str.charAt(ix + 2) == 'Y' ||
               str.charAt(ix + 2) == 'I')) {
            ch = 'J';
            ix += 2; // skip over next
          } else
            ch = 'T';
          break;

        case 'G':
          // G ->     silent if in -gh- and not at end or before a vowel
          //          in -gn- or -gned- (also see dge etc. above)
          //      J   if before i or e or y if not double gg
          //      K   otherwise
          // Initial  gn- pn, ae- or wr-      -> drop first letter
          ch = 'K';
          if (ix == 0 && str.length() > 1 && str.charAt(ix + 1) == 'N')
            ch = ' ';
          else if (ix + 1 < str.length() && str.charAt(ix + 1) == 'H') {
            if (ix + 2 == str.length() ||
                (ix + 2 < str.length() &&
                 isVowel(str.charAt(ix + 2)))) { // not at end
              ch = ' '; // skip
              ix++; // skip the 'H', too
            }
          } else if (ix + 1 < str.length() && str.charAt(ix + 1) == 'N')
            ch = ' '; // skip
          else if (ix + 1 < str.length() && (str.charAt(ix + 1) == 'I' ||
                                               str.charAt(ix + 1) == 'E' ||
                                               str.charAt(ix + 1) == 'Y') &&
                     (ix == 0 || str.charAt(ix - 1) != 'G'))
            ch = 'J';
          
          break;

        case 'H':
          // H ->     silent if after vowel and no vowel follows
          //      H   otherwise
          if (ix > 0 && isVowel(str.charAt(ix - 1)) &&
              ix + 1 < str.length() && !isVowel(str.charAt(ix + 1)))
            ch = ' '; // silent
          break;

        case 'K':
          // K ->     silent if after "c"
          //      K   otherwise
          // Initial  kn-, gn- pn, ae- or wr-      -> drop first letter
          if ((ix > 0 && str.charAt(ix - 1) == 'C') ||
              (ix == 0 && str.length() > 1 && str.charAt(ix + 1) == 'N'))
            ch = ' '; // silent
          break;

        case 'P':
          // P -> F   if before "h"
          //      P   otherwise
          // Initial  pn, ae- or wr-      -> drop first letter
          if (ix == 0 && str.length() > 1 && str.charAt(ix + 1) == 'N')
            ch = ' ';
          else if (ix + 1 < str.length() && str.charAt(ix + 1) == 'H') {
            ch = 'F';
            ix++; // skip the following 'H'
          }
          break;

        case 'Q':
          ch = 'K';
          break;

        case 'S':
          // S -> X   (sh) if before "h" or in -sio- or -sia-
          //      S   otherwise
          if ((ix + 1 < str.length() && str.charAt(ix + 1) == 'H') ||
              (ix + 2 < str.length() && str.charAt(ix + 1) == 'I' &&
               (str.charAt(ix + 2) == 'O' || str.charAt(ix + 2) == 'A'))) {
            ch = 'X';
            ix++; // skip the 'H', too
          }
          break;

        case 'T':
          // T -> X   (sh) if -tia- or -tio-
          //      0   (th) if before "h"
          //          silent if in -tch-
          //      T   otherwise
          if (ix + 2 < str.length() && str.charAt(ix + 1) == 'I' &&
              (str.charAt(ix + 2) == 'A' || str.charAt(ix + 2) == 'O'))
            ch = 'X';
          else if (ix + 1 < str.length() && str.charAt(ix + 1) == 'H') {
            ch = '0';
            ix++; // skip the 'H'
          } else if (ix + 2 < str.length() && str.charAt(ix + 1) == 'C' &&
                   str.charAt(ix + 2) == 'H')
            ch = ' ';
          break;

        case 'V':
          ch = 'F';
          break;

        case 'W':
          // W ->     silent if not followed by a vowel
          //      W   if followed by a vowel
          // Initial  wh-                          -> change to "w"
          // Initial  wr-      -> drop first letter
          if (ix == 0 && str.length() > 1 && str.charAt(ix + 1) == 'H')
            ix++; // skip the 'H'
          else if (ix == 0 && str.length() > 1 && str.charAt(ix + 1) == 'R')
            ch = ' '; // drop the 'W'
          else if (ix + 1 < str.length() && !isVowel(str.charAt(ix + 1)))
            ch = ' ';
          break;

        case 'X':
          // Initial  x-                           -> change to "s"
          if (ix > 0)
            key[pos++] = 'K';
          ch = 'S';
          break;

        case 'Y':
          // Y ->     silent if not followed by a vowel
          //      Y   if followed by a vowel
          if ((ix + 1 < str.length() && !isVowel(str.charAt(ix + 1))) ||
              ix + 1 == str.length())
            ch = ' ';
          break;

        case 'Z':
          ch = 'S';
        }
      }

      if (ch != ' ')
        key[pos++] = ch;
    }
    
    return new String(key, 0, pos);
  }

  private static boolean isVowel(char ch) {
    return (ch == 'A' || ch == 'E' || ch == 'I' || ch == 'O' || ch == 'U' ||
            ch == 'Y');
  }
}