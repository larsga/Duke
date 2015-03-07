
package no.priv.garshol.duke.cleaners;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class PhoneNumberCleanerTest {
  private PhoneNumberCleaner cleaner;

  @Before
  public void setup() {
    cleaner = new PhoneNumberCleaner();
  }
  
  @Test
  public void testEmpty() {
    assertTrue(cleaner.clean("") == null);
  }
  
  @Test
  public void testPathological() {
    test("123", "123");
  }

  @Test
  public void testUKInitialZero() {
    test("+44 020 77921414", "+44 2077921414");
  }

  @Test
  public void testNorwaySpace() {
    test("+47 23 155100", "+47 23155100");
  }

  @Test
  public void testNorwayWithoutCode() {
    test("23 21 20 00", "23212000");
  }

  @Test
  public void testZeroZeroSweden() {
    test("00 46 8 506 16100", "+46 850616100");
  }

  @Test
  public void testZeroZeroGermany() {
    test("00 49 30 881 3001", "+49 308813001");
  }

  @Test
  public void testUSNumber() {
    test("+ 1 212 554 6120", "+1 2125546120");
  }
  
  @Test
  public void testSwedenInitialZero() {
    test("+46 (0)31 751 5300 ", "+46 317515300");
  }
  
  @Test
  public void testFinland() {
    test("+358 40 7600231", "+358 407600231");
  }
  
  @Test
  public void testParenthesis() {
    test("0047 (0)55551000", "+47 55551000");
  }
  
  private void test(String value, String result) {
    assertEquals(result, cleaner.clean(value));
  }
}