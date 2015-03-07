
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.comparators.DifferentComparator;

public class DifferentComparatorTest {
  private DifferentComparator comp;

  @Before
  public void setup() {
    this.comp = new DifferentComparator();
  }

  @Test
  public void testEmpty() {
    assertEquals(0.0, comp.compare("", ""));
  }

  @Test
  public void testEmpty1() {
    assertEquals(1.0, comp.compare("", "1"));
  }

  @Test
  public void testEmpty2() {
    assertEquals(1.0, comp.compare("1", ""));
  }

  @Test
  public void testSame() {
    assertEquals(0.0, comp.compare("same", "same")); // but different
  }
}