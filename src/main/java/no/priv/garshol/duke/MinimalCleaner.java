
package no.priv.garshol.duke;

/**
 * Performs the absolute minimum of cleaning.
 */
public class MinimalCleaner implements Cleaner {

  /**
   * Returns a cleaned value.
   */
  public String clean(String value) {
    value = value.trim();
    if (value.length() == 0)
      return null;
    return value;
  }
  
}