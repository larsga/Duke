
package no.priv.garshol.duke.datasources;

import no.priv.garshol.duke.Cleaner;

public class Column {
  private String name;
  private String property;
  private String prefix;
  private Cleaner cleaner;

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
}