
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.cleaners.PersonNameCleaner;

public class PersonNameCleanerTest extends LowerCaseNormalizeCleanerTest {

  @Before
  public void setUp() {
    cleaner = new PersonNameCleaner();
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
  public void testMapping() {
    assertEquals("joseph stalin",
                 cleaner.clean("Joe Stalin"));
  }

  @Test
  public void testAccentStripping() {
    assertEquals("male", cleaner.clean("Mal\u00E9"));
  }  
}