
package no.priv.garshol.duke.utils;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class CommandLineParserTest {
  private CommandLineParser parser;
  
  @Before
  public void setup() {
    parser = new CommandLineParser();
  }
  
  @Test
  public void testEmpty() {
    String[] args = parser.parse(new String[] {});
    assertEquals(0, args.length);
  }
  
  @Test
  public void testEmpty2() {
    parser.registerOption(new CommandLineParser.BooleanOption("test", 't'));
    String[] args = parser.parse(new String[] {});
    assertEquals(false, parser.getOptionState("test"));
    assertEquals(0, args.length);
  }

  @Test
  public void testSingleArgument() {
    String[] args = parser.parse(new String[] { "foo" });
    StringUtilsTest.assertEqual(new String[]{"foo"}, args);
  }

  @Test
  public void testSingleBooleanOption() {
    parser.registerOption(new CommandLineParser.BooleanOption("test", 't'));
    String[] args = parser.parse(new String[] { "--test", "foo" });
    assertEquals(true, parser.getOptionState("test"));
    StringUtilsTest.assertEqual(new String[] { "foo" }, args);
  }

  @Test
  public void testSingleBooleanShortOption() {
    parser.registerOption(new CommandLineParser.BooleanOption("test", 't'));
    String[] args = parser.parse(new String[] { "-t", "foo" });
    assertEquals(true, parser.getOptionState("test"));
    StringUtilsTest.assertEqual(new String[] { "foo" }, args);
  }

  @Test
  public void testSingleStringOption() {
    parser.registerOption(new CommandLineParser.StringOption("test", 't'));
    String[] args = parser.parse(new String[] { "--test=bar", "foo" });
    assertEquals("bar", parser.getOptionValue("test"));
    StringUtilsTest.assertEqual(new String[] { "foo" }, args);
  }

  @Test
  public void testSingleStringShortOption() {
    parser.registerOption(new CommandLineParser.StringOption("test", 't'));
    String[] args = parser.parse(new String[] { "-t=bar", "foo" });
    assertEquals("bar", parser.getOptionValue("test"));
    StringUtilsTest.assertEqual(new String[] { "foo" }, args);
  }

  @Test
  public void testUnknownOption() {
    try {
      parser.parse(new String[] { "-t", "foo" });
      fail("didn't catch unregistered option");
    } catch (CommandLineParser.CommandLineParserException e) {
    }
  }

  @Test
  public void testNotEnoughArguments() {
    parser.setMinimumArguments(1);
    try {
      parser.parse(new String[] {  });
      fail("didn't catch required missing argument");
    } catch (CommandLineParser.CommandLineParserException e) {
    }
  }

  @Test
  public void testNotEnoughArguments2() {
    parser.setMinimumArguments(1);
    try {
      parser.parse(new String[] { "-t" });
      fail("didn't catch required missing argument");
    } catch (CommandLineParser.CommandLineParserException e) {
    }
  }

  @Test
  public void testNoValue() {
    parser.registerOption(new CommandLineParser.StringOption("test", 't'));
   try {
      parser.parse(new String[] { "-t" });
      fail("didn't catch required missing value");
    } catch (CommandLineParser.CommandLineParserException e) {
    }
  }

  @Test
  public void testLongShortConfusion() {
    parser.registerOption(new CommandLineParser.BooleanOption("test", 't'));
    try {
      parser.parse(new String[] { "-to" });
      fail("didn't catch long name for short option");
    } catch (CommandLineParser.CommandLineParserException e) {
    }
  }

  @Test
  public void testParamsAfterFile() {
    // test for issue 90
    parser.registerOption(new CommandLineParser.BooleanOption("test", 't'));
    try {
      parser.parse(new String[] { "config", "--test" });
      fail("didn't catch parameter after file parameter");
    } catch (CommandLineParser.CommandLineParserException e) {
    }
  }

  @Test
  public void testTooManyArguments() {
    parser.setMaximumArguments(1);
    try {
      parser.parse(new String[] { "foo", "bar" });
      fail("didn't catch too many arguments");
    } catch (CommandLineParser.CommandLineParserException e) {
    }
  }
  
}