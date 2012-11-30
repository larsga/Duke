
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.comparators.QGramComparator;

public class QGramComparatorTest {
  private QGramComparator comp;

  @Before
  public void setup() {
    comp = new QGramComparator();
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
    assertEquals((4.0 / 6.0), comp.compare("abc def", "cab def"));
  }
    
  @Test
  public void testGail() {
    assertEquals((1.0 / 3.0), comp.compare("gail", "gayle"));
  }
}