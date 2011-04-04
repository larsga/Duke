
package no.priv.garshol.duke;

import org.apache.lucene.queryParser.QueryParser;

/**
 * Represents a property.
 */
public class Property {
  private String name;
  private boolean id;
  private boolean analyzed;      // irrelevant if ID
  private Comparator comparator; // irrelevant if ID
  private double high;           // irrelevant if ID
  private double low;            // irrelevant if ID
  private QueryParser parser;    // irrelevant if ID

  // used to initialize ID properties
  public Property(String name) {
    this.name = name;
    id = true;
    analyzed = false;
  }
  
  public Property(String name, Comparator comparator, double low, double high) {
    this.name = name;
    this.id = false;
    this.analyzed = comparator.isTokenized();
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
   * Returns the probability that the records v1 and v2 came from represent
   * the same entity, based on high and low probability settings etc.
   */
  public double compare(String v1, String v2) {
    if (v1.equals(v2))
      return high;
    else
      return low;
  }

  public String toString() {
    return "[Property " + name + "]";
  }

  protected void setParser(QueryParser parser) {
    this.parser = parser;
  }

  protected QueryParser getParser() {
    return parser;
  }
}