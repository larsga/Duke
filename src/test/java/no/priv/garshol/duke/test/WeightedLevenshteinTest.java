
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.comparators.WeightedLevenshtein;

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
}