
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.Comparator;

/**
 * An implementation of the Soundex algorithm, and a comparator which
 * considers strings to have a score of 0.9 if their Soundex values
 * match.
 */
public class SoundexComparator implements Comparator {
  // this table is keyed 0-25 (for 'a' to 'z') to the numeric value to put
  // in the key. 0 means the letter is to be omitted.
  private static char[] number = buildTable();

  public double compare(String s1, String s2) {
    if (s1.equals(s2))
      return 1.0;

    if (soundex(s1).equals(soundex(s2)))
      return 0.9;

    return 0.0;
  }

  public boolean isTokenized() {
    return true; // I guess?
  }
  
  /**
   * Produces the Soundex key for the given string.
   */
  public static String soundex(String str) {
    if (str.length() < 1)
      return ""; // no soundex key for the empty string (could use 000)
    
    char[] key = new char[4];
    key[0] = str.charAt(0);
    int pos = 1;
    char prev = '0';
    for (int ix = 1; ix < str.length() && pos < 4; ix++) {
      char ch = str.charAt(ix);
      int charno;
      if (ch >= 'A' && ch <= 'Z')
        charno = ch - 'A';
      else if (ch >= 'a' && ch <= 'z')
        charno = ch - 'a';
      else
        continue;

      if (number[charno] != '0' && number[charno] != prev)
        key[pos++] = number[charno];
      prev = number[charno];
    }

    for ( ; pos < 4; pos++)
      key[pos] = '0';

    return new String(key);
  }

  /**
   * Builds the mapping table.
   */
  private static char[] buildTable() {
    char[] table = new char[26];
    for (int ix = 0; ix < table.length; ix++)
      table[ix] = '0';
    table['B' - 'A'] = '1';
    table['P' - 'A'] = '1';
    table['F' - 'A'] = '1';
    table['V' - 'A'] = '1';
    table['C' - 'A'] = '2';
    table['S' - 'A'] = '2';
    table['K' - 'A'] = '2';
    table['G' - 'A'] = '2';
    table['J' - 'A'] = '2';
    table['Q' - 'A'] = '2';
    table['X' - 'A'] = '2';
    table['Z' - 'A'] = '2';
    table['D' - 'A'] = '3';
    table['T' - 'A'] = '3';
    table['L' - 'A'] = '4';
    table['M' - 'A'] = '5';
    table['N' - 'A'] = '5';
    table['R' - 'A'] = '6';
    return table;
  }
}