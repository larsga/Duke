
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class LowerCaseNormalizeCleanerTest {
  protected Cleaner cleaner;

  @Before
  public void setUp() {
    cleaner = new LowerCaseNormalizeCleaner();
  }

  @Test
  public void testEmpty() {
    assertEquals("", cleaner.clean(""));
  }

  @Test
  public void testSingleChar() {
    assertEquals("a", cleaner.clean("A"));
  }

  @Test
  public void testSingleChar2() {
    assertEquals("a", cleaner.clean("a"));
  }

  @Test
  public void testSingleSpace() {
    assertEquals("", cleaner.clean(" "));
  }

  @Test
  public void testManySpaces() {
    assertEquals("", cleaner.clean("    "));
  }

  @Test
  public void testManyLeadingSpaces() {
    assertEquals("a", cleaner.clean("    a"));
  }

  @Test
  public void testManyTrailingSpaces() {
    assertEquals("a", cleaner.clean("a    "));
  }

  @Test
  public void testLarsMarius() {
    assertEquals("lars marius", cleaner.clean("Lars Marius"));
  }

  @Test
  public void testLarsMarius3Spaces() {
    assertEquals("lars marius", cleaner.clean("Lars   Marius"));
  }

  @Test
  public void testLarsMariusPadded() {
    assertEquals("lars marius", cleaner.clean("   Lars   Marius   "));
  }

  @Test
  public void testRealData() {
    assertEquals("inger elisabeth foyn havre",
                 cleaner.clean("Inger Elisabeth Foyn Havre"));
  }

  @Test
  public void testAccentStripping() {
    assertEquals("male", cleaner.clean("Mal\u00E9"));
  }

  @Test
  public void testAccentStripping2() {
    assertEquals("h\u00F8ybr\u00E5ten", cleaner.clean("H\u00F8ybr\u00E5ten"));
  }
  
}