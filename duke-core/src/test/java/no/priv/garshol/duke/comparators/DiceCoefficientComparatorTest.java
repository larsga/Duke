
package no.priv.garshol.duke.comparators;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class DiceCoefficientComparatorTest {
  private DiceCoefficientComparator comp;

  @Before
  public void setup() {
    comp = new DiceCoefficientComparator();
  }
    
  @Test
  public void testEmpty() {
    assertEquals(1.0, comp.compare("", ""));
  }
    
  @Test
  public void testOneIsEmpty() {
    assertEquals(0.0, comp.compare("", "abc"));
  }
    
  @Test
  public void testOneIsDifferent() {
    assertEquals(0.5, comp.compare("abc def", "cba def"));
  }
    
  @Test
  public void testReordering() {
    assertEquals(1.0, comp.compare("def abc", "abc def"));
  }
    
  @Test
  public void testLengthDifference() {
    assertEquals(0.8, comp.compare("def abc ghe", "abc def"));
  }
    
  @Test
  public void testLengthDifference2() {
    assertEquals(0.8, comp.compare("def abc", "abc def ghe"));
  }
}