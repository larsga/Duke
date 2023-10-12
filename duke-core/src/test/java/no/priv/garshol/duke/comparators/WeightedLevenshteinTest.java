
package no.priv.garshol.duke.comparators;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class WeightedLevenshteinTest {
  private WeightedLevenshtein.DefaultWeightEstimator e;

  @Before
  public void setup() {
    e = new WeightedLevenshtein.DefaultWeightEstimator();
  }

  @Test
  public void testEmpty() {
    assertEquals(0.0, WeightedLevenshtein.distance("", "", e));
  }

  @Test
  public void testEmpty1() {
    e.setDigitWeight(1.0);
    assertEquals(1.0, WeightedLevenshtein.distance("", "1", e));
  }

  @Test
  public void testEmpty2() {
    e.setDigitWeight(2.0);
    assertEquals(2.0, WeightedLevenshtein.distance("1", "", e));
  }

  @Test
  public void testSubstitute1() {
    e.setDigitWeight(2.0);
    assertEquals(2.0, WeightedLevenshtein.distance("titanic 1", "titanic 2", e));
  }

  @Test
  public void testSubstitute2() {
    e.setDigitWeight(2.0);
    assertEquals(3.0, WeightedLevenshtein.distance("totanic 1", "titanic 2", e));
  }

  @Test
  public void testAbc() {
    assertEquals(2.0, WeightedLevenshtein.distance("abc", "a", e));
    assertEquals(2.0, WeightedLevenshtein.distance("a", "abc", e));
  }

  @Test
  public void test123() {
    e.setDigitWeight(2.0);
    assertEquals(4.0, WeightedLevenshtein.distance("1", "123", e));
    assertEquals(4.0, WeightedLevenshtein.distance("123", "1", e));
  }

  @Test
  public void testAlphaNumeric() {
    e.setDigitWeight(2.0);
    assertEquals(8.0, WeightedLevenshtein.distance("a2c3e", "1b1d1", e));
    assertEquals(8.0, WeightedLevenshtein.distance("1b1d1", "a2c3e", e));
  }

  @Test
  public void testComparator() {
    WeightedLevenshtein comp = new WeightedLevenshtein();
    assertEquals(0.0, comp.compare("1", ""));
  }
}