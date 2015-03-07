
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.cleaners.RegexpCleaner;

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

  private void test(String regexp, String value, String result) {
    RegexpCleaner cl = new RegexpCleaner();
    cl.setRegexp(regexp);
    assertEquals(result, cl.clean(value));
  }
}