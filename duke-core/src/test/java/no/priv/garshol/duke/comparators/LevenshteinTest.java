
package no.priv.garshol.duke.comparators;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class LevenshteinTest {
  private Levenshtein comp;

  @Before
  public void setup() {
    this.comp = new Levenshtein();
  }
  
  // tests for the comparator

  @Test
  public void testComparatorEqual() {
    assertEquals(1.0, comp.compare("foo", "foo"));
  }

  @Test
  public void testComparatorTotallyDifferent() {
    assertTrue(comp.compare("foo", "bar") < 0.5);
  }

  @Test
  public void testComparatorOneInFour() {
    assertEquals(0.75, comp.compare("fooz", "foos"));
  }
  
  // tests for the original algorithm
  
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

  // tests for the compact version of the algorithm, with cutoff

  @Test
  public void testCEmpty() {
    assertEquals(0, Levenshtein.compactDistance("", ""));
  }

  @Test
  public void testCEmpty1() {
    assertEquals(1, Levenshtein.compactDistance("", "1"));
  }

  @Test
  public void testCEmpty2() {
    assertEquals(1, Levenshtein.compactDistance("1", ""));
  }

  @Test
  public void testCKitten() {
    assertEquals(3, Levenshtein.compactDistance("kitten", "sitting"));
    assertEquals(3, Levenshtein.compactDistance("sitting", "kitten"));
  }

  @Test
  public void testCDays() {
    assertEquals(3, Levenshtein.compactDistance("saturday", "sunday"));
    assertEquals(3, Levenshtein.compactDistance("sunday", "saturday"));
  }
  
  @Test
  public void testCGambol() {
    assertEquals(2, Levenshtein.compactDistance("gambol", "gumbo"));
    assertEquals(2, Levenshtein.compactDistance("gumbo", "gambol"));
  }

  @Test
  public void testCTotallyUnlike() {
    // the edit distance is 4, but we will return only 3, because of the cutoff
    assertEquals(3, Levenshtein.compactDistance("abcd", "efgh"));
  }
}