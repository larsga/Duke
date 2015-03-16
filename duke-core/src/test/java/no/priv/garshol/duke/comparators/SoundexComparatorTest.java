
package no.priv.garshol.duke.comparators;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class SoundexComparatorTest {
  private SoundexComparator comp;

  // ----- TEST CODE

  @Test
  public void testEmpty() {
    check("", "");
  }

  @Test
  public void testSue() {
    check("S000", "Sue");
  }

  @Test
  public void testSUE() {
    check("S000", "SUE");
  }

  @Test
  public void testGarshol() {
    check("G624", "Garshol");
  }

  @Test
  public void testGARSHOL() {
    check("G624", "GARSHOL");
  }

  @Test
  public void testGarskol() {
    check("G624", "Garskol");
  }

  @Test
  public void testGARSKOL() {
    check("G624", "GARSKOL");
  }
  
  private void check(String key, String value) {
    assertEquals("wrong key for '" + value + "'",
                 key, SoundexComparator.soundex(value));
  }

  // ----- TEST COMPARISON

  @Before
  public void setup() {
    comp = new SoundexComparator();
  }

  @Test
  public void testEqual() {
    assertEquals("wrong score for equal values", 1.0,
                 comp.compare("LMG", "LMG"));
  }

  @Test
  public void testEqualCode() {
    assertEquals("wrong score for values with equal codes", 0.9,
                 comp.compare("Garshol", "Garskol"));
  }

  @Test
  public void testDifferentCode() {
    assertEquals("wrong score for values with different codes", 0.0,
                 comp.compare("Garshol", "Sue"));
  }
}