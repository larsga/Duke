
package no.priv.garshol.duke;

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
    return property;
  }

  public String getPrefix() {
    return prefix;
  }

  public Cleaner getCleaner() {
    return cleaner;
  }
}