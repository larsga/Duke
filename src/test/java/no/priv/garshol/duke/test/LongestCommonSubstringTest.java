
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.comparators.LongestCommonSubstring;

public class LongestCommonSubstringTest {
  private LongestCommonSubstring comp;

  @Before
  public void setup() {
    this.comp = new LongestCommonSubstring();
  }
  
  // tests for the comparator

  @Test
  public void testEmpty() {
    assertEquals(1.0, comp.compare("", ""));
  }

  @Test
  public void testEmpty2() {
    assertEquals(0.0, comp.compare("", "foo"));
  }

  @Test
  public void testComparatorEqual() {
    assertEquals(1.0, comp.compare("foo", "foo"));
  }

  @Test
  public void testComparatorTotallyDifferent() {
    assertEquals(0.0, comp.compare("foo", "bar"));
  }

  @Test
  public void testChristen5_21() {
    assertEquals(8 / (double) 14, comp.compare("peter christen", "christian pedro"));
  }

  @Test
  public void testChristen5_21b() {
    comp.setMinimumLength(4);
    assertEquals(6 / (double) 14, comp.compare("peter christen", "christian pedro"));
  }

  @Test
  public void testSelf() {
    assertEquals(1.0, comp.compare("lars marius garshol", "lars garshol"));
  }

  @Test
  public void testSymmetric() {
    assertEquals(comp.compare("papr", "prap"),
                 comp.compare("prap", "papr"));
  }
}