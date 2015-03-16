
package no.priv.garshol.duke.comparators;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

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
  public void testChristen5_21c() {
    comp.setMinimumLength(4);
    assertEquals(13 / (double) 14, comp.compare("peter christen", "christen peter"));
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

  @Test
  public void testHotels() {
    assertEquals(8 / (double) 11, 
                 comp.compare("the waldorf hilton", "one aldwych"));
  }

  @Test
  public void testHotels2() {
    comp.setMinimumLength(3);
    assertEquals(6 / (double) 11, 
                 comp.compare("the waldorf hilton", "one aldwych"));
  }

  @Test
  public void testJaccard1() {
    comp.setFormula(LongestCommonSubstring.Formula.JACCARD);
    assertEquals(8 / (double) 21, comp.compare("peter christen", "christian pedro"));
  }  

  @Test
  public void testJaccard2() {
    comp.setFormula(LongestCommonSubstring.Formula.JACCARD);
    assertEquals(13 / (double) 15, comp.compare("peter christen", "christen peter"));
  }  

  @Test
  public void testDice1() {
    comp.setFormula(LongestCommonSubstring.Formula.DICE);
    assertEquals(16 / (double) 29, comp.compare("peter christen", "christian pedro"));
  }  

  @Test
  public void testDice2() {
    comp.setFormula(LongestCommonSubstring.Formula.DICE);
    assertEquals(13 / (double) 14, comp.compare("peter christen", "christen peter"));
  }  
}