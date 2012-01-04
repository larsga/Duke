
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
  public void testMapping() {
    assertEquals("joseph stalin",
                 cleaner.clean("Joe Stalin"));
  }
}