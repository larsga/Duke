
package no.priv.garshol.duke.test;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.JaroWinkler;

public class JaroWinklerTest {

  @Test
  public void testEmpty() {
    assertEquals(1.0, JaroWinkler.similarity("", ""));
  }

  @Test
  public void testEqual() {
    assertEquals(1.0, JaroWinkler.similarity("abc", "abc"));
  }

  @Test
  public void testTotallyDifferent() {
    assertEquals(0.0, JaroWinkler.similarity("abc", "def"));
  }

  @Test
  public void testWikipedia1() {
    double score = (4/6.0 + 4/5.0 + (4-0)/4.0)/3.0;
    assertEquals(score, JaroWinkler.similarity("DwAyNE", "DuANE"));
  }

  @Test
  public void testWikipedia2() {
    double score = (6/6.0 + 6/6.0 + (6-1)/6.0)/3.0;
    assertEquals(score, JaroWinkler.similarity("MARTHA", "MARHTA"));
  }

  @Test
  public void testWikipedia3() {
    double score = (3/5.0 + 3/5.0 + (3-0)/3.0)/3.0;
    assertEquals(score, JaroWinkler.similarity("CRATE", "TRACE"));
  }

  @Test
  public void testWikipedia4() {
    double score = (4/5.0 + 4/8.0 + (4-0)/4.0)/3.0;
    assertEquals(score, JaroWinkler.similarity("DIXON", "DICKSONX"));
  }

  @Test
  public void testYancey1() {
    double score = (5/6.0 + 5/8.0 + (5-1)/5.0)/3.0;
    assertEquals(score, JaroWinkler.similarity("anderson", "barnes"));
  }
  
}