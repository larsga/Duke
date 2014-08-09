
package no.priv.garshol.duke.datasources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import no.priv.garshol.duke.Cleaner;

public class Column {
  private String name;
  private String property;
  private String prefix;
  private Cleaner cleaner;
  private Pattern splitter;

  public Column(String name, String property, String prefix, Cleaner cleaner) {
    this.name = name;
    this.property = property;
    this.prefix = prefix;
    this.cleaner = cleaner;
  }

  public String getName() {
    return name;
  }

  public String getProperty() {
    if (property == null)
      return name;
    else
      return property;
  }

  public String getPrefix() {
    return prefix;
  }

  public Cleaner getCleaner() {
    return cleaner;
  }

  public void setSplitOn(String spliton) {
    this.splitter = Pattern.compile(spliton);
  }

  /**
   * Returns true iff this column needs to be split into multiple values.
   */
  public boolean isSplit() {
    return splitter != null;
  }

  /**
   * Splits the given string into multiple values.
   */
  public Collection<String> split(String value) {
    String[] parts = splitter.split(value);
    Collection<String> values = new ArrayList(parts.length);
    for (int ix = 0; ix < parts.length; ix++)
      values.add(parts[ix]);
    return values;
  }

  public String getSplitOn() {
	return splitter.toString();
  }
}