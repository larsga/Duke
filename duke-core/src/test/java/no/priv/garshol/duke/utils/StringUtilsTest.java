
package no.priv.garshol.duke.utils;

import junit.framework.AssertionFailedError;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class StringUtilsTest {

  // --- replaceAnyOf

  @Test
  public void testEmptyReplace() {
    assertEquals("", StringUtils.replaceAnyOf("", "abc", ' '));
  }

  @Test
  public void testNoReplacements() {
    assertEquals("123", StringUtils.replaceAnyOf("123", "abc", ' '));
  }

  @Test
  public void testAllReplacements() {
    assertEquals("   ", StringUtils.replaceAnyOf("abc", "abc", ' '));
  }
  
  // --- normalizeWS

  @Test
  public void testEmptyWS() {
    assertEquals("", StringUtils.normalizeWS(""));
  }

  @Test
  public void testNoWS() {
    assertEquals("abc", StringUtils.normalizeWS("abc"));
  }

  @Test
  public void testLeadingWS() {
    assertEquals("abc", StringUtils.normalizeWS("  abc"));
  }

  @Test
  public void testTrailingWS1() {
    assertEquals("abc", StringUtils.normalizeWS("abc "));
  }
  
  @Test
  public void testTrailingWS2() {
    assertEquals("abc", StringUtils.normalizeWS("abc  "));
  }

  @Test
  public void testTrailingWS3() {
    assertEquals("abc def", StringUtils.normalizeWS("abc def "));
  }
  
  // --- split

  @Test
  public void testEmpty() {
    assertEqual(new String[] {}, StringUtils.split(""));
  }

  @Test
  public void testOneToken() {
    assertEqual(new String[] {"ab"}, StringUtils.split("ab"));
  }

  @Test
  public void testTwoTokens() {
    assertEqual(new String[] {"a", "b"}, StringUtils.split("a b"));
  }

  @Test
  public void testTwoTokensLeadingTrailing() {
    assertEqual(new String[] {"a", "b"}, StringUtils.split("  a b  "));
  }

  @Test
  public void testManySpaces() {
    assertEqual(new String[] {"aaa", "bbb"}, StringUtils.split("aaa   bbb"));
  }

  @Test
  public void testThreeTokens() {
    assertEqual(new String[] {"aaa", "bbb", "ccc"},
                StringUtils.split("aaa bbb ccc"));
  }
  
  public static void assertEqual(String[] s1, String[] s2) {
    boolean equal = s1.length == s2.length;
    for (int ix = 0; ix < s1.length && equal; ix++)
      equal = s1[ix].equals(s2[ix]);

    if (!equal)
      throw new AssertionFailedError("Array " + CSVReaderTest.toString(s1) +
                                     " != " + CSVReaderTest.toString(s2));
  }
}