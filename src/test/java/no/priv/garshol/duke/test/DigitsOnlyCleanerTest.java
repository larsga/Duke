
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.cleaners.DigitsOnlyCleaner;

public class DigitsOnlyCleanerTest {
  private DigitsOnlyCleaner cleaner;

  @Before
  public void setup() {
    cleaner = new DigitsOnlyCleaner();
  }
  
  @Test
  public void testEmpty() {
    test("", "");
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