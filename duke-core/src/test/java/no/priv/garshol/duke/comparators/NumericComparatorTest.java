
package no.priv.garshol.duke.comparators;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class NumericComparatorTest {
  private NumericComparator comp;

  @Before
  public void setUp() {
    comp = new NumericComparator();
  }

  @Test
  public void testEqual() {
    assertEquals(1.0, comp.compare("42", "42"));
  }

  @Test
  public void testEqual2() {
    assertEquals(1.0, comp.compare("42.0", "42.0"));
  }

  @Test
  public void testHalf() {
    assertEquals(0.5, comp.compare("21.0", "42.0"));
  }

  @Test
  public void testHalfInverted() {
    assertEquals(0.5, comp.compare("42.0", "21.0"));
  }

  @Test
  public void testHalfBelowMin() {
    comp.setMinRatio(0.75);
    assertEquals(0.0, comp.compare("21.0", "42.0"));
  }

  @Test
  public void testHalfAboveMin() {
    comp.setMinRatio(0.25);
    assertEquals(0.5, comp.compare("21.0", "42.0"));
  }

  @Test
  public void testZero() {
    assertEquals(1.0, comp.compare("0.0", "0.0"));
  }

  @Test
  public void testFirstIsZero() {
    assertEquals(0.0, comp.compare("0.0", "42.0"));
  }

  @Test
  public void testSecondIsZero() {
    assertEquals(0.0, comp.compare("42.0", "0.0"));
  }

  @Test
  public void testOneNegativeOnePositive() {
    assertEquals(0.0, comp.compare("-1", "2"));
  }

  @Test
  public void testNegativeNumbers() {
    assertEquals(0.5, comp.compare("-1", "-2"));
    assertEquals(0.5, comp.compare("-2", "-1"));
  }
}
