
package no.priv.garshol.duke.test;

import org.junit.Test;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.SoundexComparator;

public class SoundexComparatorTest {

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
}