
package no.priv.garshol.duke.cleaners;

import no.priv.garshol.duke.Cleaner;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HTMLCleanerTest {
  protected Cleaner cleaner;

  @Before
  public void setUp() {
    cleaner = new HTMLCleaner();
  }

  @Test
  public void testEmpty() {
    assertEquals("", cleaner.clean(""));
  }

  @Test
  public void testSingleChar() {
    assertEquals("a", cleaner.clean("a"));
  }

  @Test
  public void testSingleEntity() {
    assertEquals("ralf hartmut g\u00FCting",
                 cleaner.clean("ralf hartmut g&#252;ting"));
  }

  @Test
  public void testFirst() {
    assertEquals("ABC",
                 cleaner.clean("&#65;BC"));
  }

  @Test
  public void testLast() {
    assertEquals("ABC",
                 cleaner.clean("AB&#67;"));
  }

  @Test
  public void testSingleNamedEntity() {
    assertEquals("the vldb journal \u2014 the international journal on very large data bases",
                 cleaner.clean("the vldb journal &mdash; the international journal on very large data bases"));
  }

  @Test
  public void testThreeEntities() {
    assertEquals("ricardo jim\u00e9nez-peris, m. pati\u00f1o-mart\u00ednez, gustavo alonso, bettina kemme",
                 cleaner.clean("ricardo jim&#233;nez-peris, m. pati&#241;o-mart&#237;nez, gustavo alonso, bettina kemme"));
  }
}
