
package no.priv.garshol.duke;

import org.apache.lucene.queryParser.QueryParser;

/**
 * Represents a property.
 */
public class Property {
  private String name;
  private boolean id;
  private boolean analyzed;      // irrelevant if ID
  private boolean ignore;        // irrelevant if ID
  private Comparator comparator; // irrelevant if ID
  private double high;           // irrelevant if ID
  private double low;            // irrelevant if ID

  // used to initialize ID properties
  public Property(String name) {
    this.name = name;
    this.id = true;
    this.analyzed = false;
  }
  
  public Property(String name, Comparator comparator, double low, double high) {
    this.name = name;
    this.id = false;
    this.analyzed = comparator != null && comparator.isTokenized();
    this.comparator = comparator;
    this.high = high;
    this.low = low;
  }
  
  // FIXME: rules for property names?
  public String getName() {
    return name;
  }
 
  // these are not used for matching. however, should we perhaps make a
  // privileged property? we must have some concept of identity.
  public boolean isIdProperty() {
    return id;
  }

  public boolean isAnalyzedProperty() {
    return analyzed;
  }

  public Comparator getComparator() {
    return comparator;
  }

  public double getHighProbability() {
    return high;
  }

  public double getLowProbability() {
    return low;
  }

  /**
   * Sets the comparator used for this property. Note that changing
   * this while Duke is processing may have unpredictable
   * consequences.
   */
  public void setComparator(Comparator comparator) {
    this.comparator = comparator;
  }

  /**
   * Sets the high probability used for this property. Note that
   * changing this while Duke is processing may have unpredictable
   * consequences.
   */
  public void setHighProbability(double high) {
    this.high = high;
  }

  /**
   * Sets the low probability used for this property. Note that
   * changing this while Duke is processing may have unpredictable
   * consequences.
   */
  public void setLowProbability(double low) {
    this.low = low;
  }

  /**
   * Iff true the property should not be used for comparing records.
   */
  public boolean isIgnoreProperty() {
    // some people set high probability to zero, which means these
    // properties will prevent any matches from occurring at all if
    // we try to use them. so we skip these.
    return ignore || high == 0.0;
  }
  
  /**
   * Makes Duke skip this property when comparing records.
   */
  public void setIgnoreProperty(boolean ignore) {
    this.ignore = ignore;
  }
  
  /**
   * Returns the probability that the records v1 and v2 came from represent
   * the same entity, based on high and low probability settings etc.
   */
  public double compare(String v1, String v2) {
    // FIXME: it should be possible here to say that, actually, we
    // didn't learn anything from comparing these two values, so that
    // probability is set to 0.5.

    if (comparator == null)
      return 0.5; // we ignore properties with no comparator
    
    double sim = comparator.compare(v1, v2);
    if (sim >= 0.5)
      return ((high - 0.5) * (sim * sim)) + 0.5;
    else
      return low;
  }

  public String toString() {
    return "[Property " + name + "]";
  }
}
