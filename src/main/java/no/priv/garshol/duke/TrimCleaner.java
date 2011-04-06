
package no.priv.garshol.duke;

public class TrimCleaner implements Cleaner {

  public String clean(String value) {
    value = value.trim();
    if (value.equals(""))
      return null;
    return value;
  }
  
}