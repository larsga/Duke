
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.cleaners.NorwegianCompanyNameCleaner;

public class NorwegianCompanyNameCleanerTest {
  private NorwegianCompanyNameCleaner cleaner;
  
  @Before
  public void setup() {
    cleaner = new NorwegianCompanyNameCleaner();
  }
  
  @Test
  public void testEmpty() {
    test("", "");
  }

  @Test
  public void testAslashsAs() {
    test("sundby maskin as", "sundby maskin a/s");
  }

  @Test
  public void testAbackslashAs() {
    test("sundby maskin as", "sundby maskin a\\s");
  }

  @Test
  public void testAslashL() {
    test("al follestadgata sameie", "a/l follestadgata sameie");
  }

  @Test
  public void testMoveALToEnd() {
    test("a/l follestadgata sameie", "follestadgata sameie al");
  }

  @Test
  public void testMoveASToEnd() {
    test("a/s sundby maskin", "sundby maskin as");
  }
  
  private void test(String s1, String s2) {
    assertEquals(cleaner.clean(s1), cleaner.clean(s2));
  }
  
}