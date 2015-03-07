
package no.priv.garshol.duke.cleaners;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class PersonNameCleanerTest extends LowerCaseNormalizeCleanerTest {

  @Before
  public void setUp() {
    cleaner = new PersonNameCleaner();
  }

  @Test
  public void testMapping() {
    assertEquals("joseph stalin",
                 cleaner.clean("Joe Stalin"));
  }

  @Test
  public void testMappingEmpty() {
    assertEquals("", cleaner.clean(""));
  }

  // @Test
  // public void testMappingNull() {
  //   assertEquals(null, cleaner.clean(null));
  // }
}