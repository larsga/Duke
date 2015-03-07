
package no.priv.garshol.duke.comparators;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class JaccardIndexComparatorTest {
  private JaccardIndexComparator comp;

  @Before
  public void setup() {
    comp = new JaccardIndexComparator();
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
    assertEquals((1.0 / 3.0), comp.compare("abc def", "cba def"));
  }
    
  @Test
  public void testSameSets() {
    assertEquals(1.0, comp.compare("abc def", "def abc"));
  }
}