
package no.priv.garshol.duke.comparators;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

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
  public void testEqual2() {
    assertEquals(1.0, JaroWinkler.similarity("ab", "ab"));
  }

  @Test
  public void testEqual1() {
    assertEquals(1.0, JaroWinkler.similarity("a", "a"));
  }

  @Test
  public void testEqual4() {
    assertEquals(1.0, JaroWinkler.similarity("abcd", "abcd"));
  }
  
  @Test
  public void testTotallyDifferent() {
    assertEquals(0.0, JaroWinkler.similarity("abc", "def"));
  }

  @Test
  public void testWikipedia1() {
    double score = (4/6.0 + 4/5.0 + (4-0)/4.0)/3.0;
    score = score + ((1 * (1 - score)) / 10); // prefix
    assertEquals(score, JaroWinkler.similarity("DwAyNE", "DuANE"));
  }

  @Test
  public void testWikipedia2() {
    double score = (6/6.0 + 6/6.0 + (6-1)/6.0)/3.0;
    score = score + ((3 * (1 - score)) / 10); // prefix
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
    score = score + ((2 * (1 - score)) / 10); // prefix
    assertEquals(score, JaroWinkler.similarity("DIXON", "DICKSONX"));
  }

  @Test
  public void testYancey1() {
    double score = (5/6.0 + 5/8.0 + (5-1)/5.0)/3.0;
    assertEquals(score, JaroWinkler.similarity("anderson", "barnes"));
  }

  // following tests from Winkler, William E. 2006. Overview of Record
  // Linkage and Current Research Directions. Statistical Research
  // Division, U.S. Census Bureau.
  // http://www.census.gov/srd/papers/pdf/rrs2006-02.pdf
  
  @Test
  public void testWinkler1() {
    roughlyEquals(0.982,
                 JaroWinkler.similarity("SHACKLEFORD", "SHACKELFORD"));
  }

  @Test
  public void testWinkler2() {
    roughlyEquals(0.896,
                 JaroWinkler.similarity("DUNNINGHAM", "CUNNIGHAM"));
  }

  @Test
  public void testWinkler3() {
    roughlyEquals(0.956,
                 JaroWinkler.similarity("NICHLESON", "NICHULSON"));
  }

  @Test
  public void testWinkler4() {
    roughlyEquals(0.832,
                 JaroWinkler.similarity("JONES", "JOHNSON"));
  }

  @Test
  public void testWinkler5() {
    roughlyEquals(0.933,
                 JaroWinkler.similarity("MASSEY", "MASSIE"));
  }

  @Test
  public void testWinkler6() {
    roughlyEquals(0.922,
                 JaroWinkler.similarity("ABROMS", "ABRAMS"));
  }

  @Test
  public void testWinkler7() {
    roughlyEquals(0.722, // winkler's table says 0.0, which makes no sense
                 JaroWinkler.similarity("HARDIN", "MARTINEZ"));
  }

  @Test
  public void testWinkler8() {
    roughlyEquals(0.467, // winkler's table says 0.0, which makes no sense
                 JaroWinkler.similarity("ITMAN", "SMITH"));
  }

  @Test
  public void testWinkler9() {
    roughlyEquals(0.926,
                 JaroWinkler.similarity("JERALDINE", "GERALDINE"));
  }

  @Test
  public void testWinkler10() {
    roughlyEquals(0.921,
                 JaroWinkler.similarity("MICHELLE", "MICHAEL"));
  }

  @Test
  public void testWinkler11() {
    roughlyEquals(0.933,
                 JaroWinkler.similarity("JULIES", "JULIUS"));
  }

  @Test
  public void testWinkler12() {
    roughlyEquals(0.88,
                 JaroWinkler.similarity("TANYA", "TONYA"));
  }

  @Test
  public void testWinkler13() {
    roughlyEquals(0.805,
                 JaroWinkler.similarity("SEAN", "SUSAN"));
  }

  @Test
  public void testWinkler14() {
    roughlyEquals(0.933,
                 JaroWinkler.similarity("JON", "JOHN"));
  }

  private void roughlyEquals(double d1, double d2) {
    assertTrue("too different: " + d1 + " != " + d2,
               Math.abs(d1 - d2) < 0.01);
  }
  
}