
package no.priv.garshol.duke.comparators;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class PersonNameComparatorTest {
  private PersonNameComparator comp;

  @Before
  public void setUp() {
    comp = new PersonNameComparator();
  }
  
  @Test
  public void testEmpty() {
    assertEquals(1.0, comp.compare("", ""));
  }

  @Test
  public void testEqual() {
    assertEquals(1.0, comp.compare("lars marius garshol", "lars marius garshol"));
  }

  @Test
  public void testNotAtAllEqual() {
    assertEquals(0.0, comp.compare("abcde fghij", "lars marius"));
    assertEquals(0.0, comp.compare("lars marius", "abcde fghij"));
  }

  @Test
  public void testInitial() {
    assertEquals(0.9, comp.compare("lars marius garshol", "lars m. garshol"));
    assertEquals(0.9, comp.compare("lars m. garshol", "lars marius garshol"));
  }

  @Test
  public void testInitialWithoutPeriod() {
    assertEquals(0.9, comp.compare("lars marius garshol", "lars m garshol"));
    assertEquals(0.9, comp.compare("lars m garshol", "lars marius garshol"));
  }
  
  @Test
  public void testMissingMiddleName() {
    assertEquals(0.8, comp.compare("lars marius garshol", "lars garshol"));
    assertEquals(0.8, comp.compare("lars garshol", "lars marius garshol"));
  }
  
  @Test
  public void testMissingInitial() {
    assertEquals(0.8, comp.compare("lars garshol", "lars m. garshol"));
    assertEquals(0.8, comp.compare("lars m. garshol", "lars garshol"));
  }
  
  @Test
  public void testMissingLeadingInitial() {
    assertEquals(0.8, comp.compare("j. william murdoch", "william murdoch"));
    assertEquals(0.8, comp.compare("william murdoch", "j. william murdoch"));
  }
  
  @Test
  public void testEditDistance() {
    assertEquals(0.95, comp.compare("lars marius garshol", "lars marus garshol"));
    assertEquals(0.95, comp.compare("lars marus garshol", "lars marius garshol"));
  }

  @Test
  public void testSingleWordDiff() {
    assertEquals(0.0, comp.compare("abcde", "lars"));
  }

  @Test
  public void testReversedOrder() {
    assertEquals(0.9, comp.compare("zhu bin", "bin zhu"));
  }

  @Test
  public void testOneCharDifference() {
    assertEquals(0.95, comp.compare("bernardo cuencagrau",
                                    "bernardo cuenca grau"));
  }

  @Test
  public void testOneCharDifference2() {
    assertEquals(0.6, comp.compare("liang du", "liang xu"));
  }

  @Test
  public void testOneCharDifference3() {
    assertEquals(0.8, comp.compare("liang gang", "liang wang"));
  }

  @Test
  public void testShortenedGivenName() {
    assertEquals(0.9, comp.compare("chris welty", "christopher welty"));
  }
  
}