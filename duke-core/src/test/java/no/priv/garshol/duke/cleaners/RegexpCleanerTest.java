
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.cleaners.RegexpCleaner;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class RegexpCleanerTest extends LowerCaseNormalizeCleanerTest {

  @Test
  public void testNoMatch() {
    test("^(\\d\\d\\d\\d)-", "gurble", null);
  }
  
  @Test
  public void testStartYear() {
    test("^(\\d\\d\\d\\d)-", "1850-1888", "1850");
  }

  @Test
  public void testEndYear() {
    test("-(\\d\\d\\d\\d)$", "1850-1888", "1888");
  }

  @Test
  public void discardSecondGroup() {
    RegexpCleaner cl = new RegexpCleaner();
    cl.setDiscardGroup(true);
    cl.setGroup(2);
    cl.setRegexp("([a-zA-Z])(\\d+)");
    assertEquals("IDontLikeDigitsBut53inTheEndIsOk", cl.clean("ID42ontLikeDigitsBut53inTheEndIsOk"));
  }

  @Test
  public void discardAll() {
    RegexpCleaner cl = new RegexpCleaner();
    cl.setDiscardGroup(false); //independent of discard flag
    cl.setDiscardAllGroup(true);
    cl.setRegexp("(\\d+)");
    assertEquals("IDontLikeDigits  $", cl.clean("I123Dont454Like450Di3gits 4234 0234$"));
  }

  @Test
  public void discardAllSecondGroup() {
    RegexpCleaner cl = new RegexpCleaner();
    cl.setDiscardAllGroup(true);
    cl.setGroup(2);
    cl.setRegexp("([A-Z])(\\d+\\s?)");
    assertEquals("This is DUKE",cl.clean("This is D1 U312 K1231 E4332"));
  }

  private void test(String regexp, String value, String result) {
    RegexpCleaner cl = new RegexpCleaner();
    cl.setRegexp(regexp);
    assertEquals(result, cl.clean(value));
  }
}