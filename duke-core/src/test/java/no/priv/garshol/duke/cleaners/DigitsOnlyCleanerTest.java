
package no.priv.garshol.duke.cleaners;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class DigitsOnlyCleanerTest {
  private DigitsOnlyCleaner cleaner;

  @Before
  public void setup() {
    cleaner = new DigitsOnlyCleaner();
  }
  
  @Test
  public void testEmpty() {
    assertTrue(cleaner.clean("") == null);
  }
  
  @Test
  public void testOnlyDigits() {
    test("314", "314");
  }
  
  @Test
  public void testDigitsAndSpaces() {
    test(" 3 1 4 ", "314");
  }
  
  private void test(String value, String result) {
    assertEquals(result, cleaner.clean(value));
  }
}