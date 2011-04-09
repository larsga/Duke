
package no.priv.garshol.duke.test;

import org.junit.Test;
import junit.framework.AssertionFailedError;

import no.priv.garshol.duke.StringUtils;

public class StringUtilsTest {

  @Test
  public void testEmpty() {
    assertEquals(new String[] {}, StringUtils.split(""));
  }

  @Test
  public void testOneToken() {
    assertEquals(new String[] {"ab"}, StringUtils.split("ab"));
  }

  @Test
  public void testTwoTokens() {
    assertEquals(new String[] {"a", "b"}, StringUtils.split("a b"));
  }

  @Test
  public void testTwoTokensLeadingTrailing() {
    assertEquals(new String[] {"a", "b"}, StringUtils.split("  a b  "));
  }

  @Test
  public void testManySpaces() {
    assertEquals(new String[] {"aaa", "bbb"}, StringUtils.split("aaa   bbb"));
  }

  @Test
  public void testThreeTokens() {
    assertEquals(new String[] {"aaa", "bbb", "ccc"},
                 StringUtils.split("aaa bbb ccc"));
  }
  
  private void assertEquals(String[] s1, String[] s2) {
    boolean equal = s1.length == s2.length;
    for (int ix = 0; ix < s1.length && equal; ix++)
      equal = s1[ix].equals(s2[ix]);

    if (!equal)
      throw new AssertionFailedError("Array " + CSVReaderTest.toString(s1) +
                                     " != " + CSVReaderTest.toString(s2));
  }
}