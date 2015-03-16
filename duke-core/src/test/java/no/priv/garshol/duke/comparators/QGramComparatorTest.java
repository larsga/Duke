
package no.priv.garshol.duke.comparators;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class QGramComparatorTest {
  private QGramComparator comp;

  @Before
  public void setup() {
    comp = new QGramComparator();
  }
    
  @Test
  public void testEmpty() {
    assertEquals(1.0, comp.compare("", ""));
  }
    
  @Test
  public void testOneIsEmpty() {
    assertEquals(0.0, comp.compare("", "abc"));
  }
    
  @Test
  public void testOneIsDifferent() {
    assertEquals((4.0 / 6.0), comp.compare("abc def", "cab def"));
  }
    
  @Test
  public void testGail() {
    assertEquals((1.0 / 3.0), comp.compare("gail", "gayle"));
  }
    
  @Test
  public void testGailJaccard() {
    comp.setFormula(QGramComparator.Formula.JACCARD);
    assertEquals((1.0 / 6.0), comp.compare("gail", "gayle"));
  }
    
  @Test
  public void testGailDice() {
    comp.setFormula(QGramComparator.Formula.DICE);
    assertEquals((2.0 / 7.0), comp.compare("gail", "gayle"));
  }

  @Test
  public void testGail3() {
    comp.setQ(3);
    assertEquals(0.0, comp.compare("gail", "gayle"));
  }

  @Test
  public void testGarshol3() {
    comp.setQ(3);
    assertEquals((4.0 / 5.0), comp.compare("garshol", "garshoel"));
  }

  @Test
  public void testGailPositional() {
    comp.setTokenizer(QGramComparator.Tokenizer.POSITIONAL);
    assertEquals((1.0 / 3.0), comp.compare("gail", "gayle"));
  }

  @Test
  public void testKakadu() {
    assertEquals((1.0 / 2.0), comp.compare("kakadu", "cacadu"));
  }

  @Test
  public void testKakaduPositional() {
    comp.setTokenizer(QGramComparator.Tokenizer.POSITIONAL);
    assertEquals((2.0 / 5.0), comp.compare("kakadu", "cacadu"));
  }

  @Test
  public void testGailEnds() {
    comp.setTokenizer(QGramComparator.Tokenizer.ENDS);
    assertEquals((2.0 / 5.0), comp.compare("gail", "gayle"));
  }
}