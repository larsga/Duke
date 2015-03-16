
package no.priv.garshol.duke.cleaners;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class TrimCleanerTest {
  private TrimCleaner cleaner;

  @Before
  public void setup() {
    cleaner = new TrimCleaner();
  }
  
  @Test
  public void testEmpty() {
    test("", null);
  }
  
  @Test
  public void testOnlyDigits() {
    test("314", "314");
  }
  
  @Test
  public void testDigitsAndSpaces() {
    test(" 3 1 4 ", "3 1 4");
  }
  
  private void test(String value, String result) {
    assertEquals(result, cleaner.clean(value));
  }
}