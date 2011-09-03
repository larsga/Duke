
package no.priv.garshol.duke.test;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.comparators.Levenshtein;

public class LevenshteinTest {

  @Test
  public void testEmpty() {
    assertEquals(0, Levenshtein.distance("", ""));
  }

  @Test
  public void testEmpty1() {
    assertEquals(1, Levenshtein.distance("", "1"));
  }

  @Test
  public void testEmpty2() {
    assertEquals(1, Levenshtein.distance("1", ""));
  }

  @Test
  public void testKitten() {
    assertEquals(3, Levenshtein.distance("kitten", "sitting"));
    assertEquals(3, Levenshtein.distance("sitting", "kitten"));
  }

  @Test
  public void testDays() {
    assertEquals(3, Levenshtein.distance("saturday", "sunday"));
    assertEquals(3, Levenshtein.distance("sunday", "saturday"));
  }
  
  @Test
  public void testGambol() {
    assertEquals(2, Levenshtein.distance("gambol", "gumbo"));
    assertEquals(2, Levenshtein.distance("gumbo", "gambol"));
  }

  @Test
  public void testTotallyUnlike() {
    assertEquals(4, Levenshtein.distance("abcd", "efgh"));
  }
  
}