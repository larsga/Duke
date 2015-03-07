
package no.priv.garshol.duke.utils;

import java.util.Map;
import java.util.HashMap;

import no.priv.garshol.duke.DukeConfigException;

public class CommandLineParser {
  private Map<String, Option> options;
  private Map<Character, Option> shortnames;
  private int minargs;
  private int maxargs;

  public CommandLineParser() {
    this.options = new HashMap();
    this.shortnames = new HashMap();
    this.maxargs = 1000000;
  }

  public String[] parse(String[] argv) throws CommandLineParserException {
    int ix = 0;
    for (; ix < argv.length; ix++) {
      if (argv[ix].charAt(0) != '-')
        break; // no more options, now on to arguments
      
      String value = null;
      int pos = argv[ix].indexOf('=');
      boolean islong = argv[ix].startsWith("--");
      String name;
      if (pos != -1) {
        value = argv[ix].substring(pos + 1);
        name = argv[ix].substring(islong ? 2 : 1, pos);
      } else
        name = argv[ix].substring(islong ? 2 : 1);
      Option option;
      if (islong)
        option = getOption(name);
      else {
        if (name.length() != 1)
          throw new CommandLineParserException("Option with single - had long" +
                                               " name: " + name);
        option = getOption(name.charAt(0));
      }

      if (option == null)
        throw new CommandLineParserException("No such option: " + argv[ix]+ " '" + name + "'");
      
      if (option instanceof BooleanOption) {
        ((BooleanOption) option).turnon();
        if (value != null)
          throw new CommandLineParserException("This option takes no value " +
                                               argv[ix]);
      } else {
        StringOption o = (StringOption) option;
        if (value == null)
          throw new CommandLineParserException("No value for option " + 
                                               argv[ix]);
        o.setValue(value);
      }
    }

    if (argv.length - ix < minargs)
      throw new CommandLineParserException("Must have at least " + minargs +
                                           " arguments; got " +
                                           (argv.length - ix));
    if (argv.length - ix > maxargs)
      throw new CommandLineParserException("Can't have more than " + maxargs +
                                           " arguments; got " +
                                           (argv.length - ix));
      
    String[] args = new String[argv.length - ix];
    for (int pos = 0; ix < argv.length; ix++) {
      // verify that user hasn't put options *after* the first file argument
      if (argv[ix].charAt(0) == '-')
        throw new CommandLineParserException("Option " + argv[ix] + " appears "+
                                             "after file arguments");
      args[pos++] = argv[ix];
    }
    return args;
  }
  
  public Option getOption(String longname) {
    return options.get(longname);
  }

  public Option getOption(char shortname) {
    return shortnames.get(shortname);
  }
  
  public boolean getOptionState(String longname) {
    return ((BooleanOption) options.get(longname)).getState();
  }
  
  public String getOptionValue(String longname) {
    return ((StringOption) options.get(longname)).getValue();
  }
  
  public int getOptionInteger(String longname, int defaultvalue) {
    String val = ((StringOption) options.get(longname)).getValue();
    if (val == null)
      return defaultvalue;
    try {
      return Integer.parseInt(val);
    } catch (NumberFormatException e) {
      throw new DukeConfigException("Option --" + longname + " must be an integer, not '" + val + "'");
    }
  }
  
  public double getOptionDouble(String longname, double defaultvalue) {
    String val = ((StringOption) options.get(longname)).getValue();
    if (val == null)
      return defaultvalue;
    try {
      return Double.parseDouble(val);
    } catch (NumberFormatException e) {
      throw new DukeConfigException("Option --" + longname + " must be a double, not '" + val + "'");
    }
  }
  
  public void setMinimumArguments(int minargs) {
    this.minargs = minargs;
  }

  public void setMaximumArguments(int maxargs) {
    this.maxargs = maxargs;
  }
  
  public void registerOption(Option option) {
    this.options.put(option.getLongname(), option);
    this.shortnames.put(option.getShortname(), option);
  }

  public void addStringOption(String longname, char shortname) {
    registerOption(new StringOption(longname, shortname));
  }

  public void addBooleanOption(String longname, char shortname) {
    registerOption(new BooleanOption(longname, shortname));
  }
  
  public abstract static class Option {
    private char shortname;
    private String longname;

    public Option(String longname) {
      this.longname = longname;
    }
    
    public Option(String longname, char shortname) {
      this.longname = longname;
      this.shortname = shortname;
    }

    public String getLongname() {
      return longname;
    }

    public char getShortname() {
      return shortname;
    }
  }

  public static class BooleanOption extends Option {
    private boolean state;

    public BooleanOption(String longname, char shortname) {
      super(longname, shortname);
    }

    public BooleanOption(String longname) {
      super(longname);
    }
    
    public boolean getState() {
      return state;
    }

    public void turnon() {
      state = true;
    }
  }

  public static class StringOption extends Option {
    private String value;

    public StringOption(String longname, char shortname) {
      super(longname, shortname);
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  public static class CommandLineParserException extends RuntimeException {
    public CommandLineParserException(String msg) {
      super(msg);
    }
  }
}
