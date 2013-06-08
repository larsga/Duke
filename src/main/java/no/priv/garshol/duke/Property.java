
package no.priv.garshol.duke;

/**
 * Represents a property.
 */
public interface Property {
  
  public String getName();
 
  // these are not used for matching. however, should we perhaps make a
  // privileged property? we must have some concept of identity.
  public boolean isIdProperty();

  public boolean isAnalyzedProperty();

  public Comparator getComparator();

  public double getHighProbability();

  public double getLowProbability();

  public Lookup getLookupBehaviour();
  
  /**
   * Sets the comparator used for this property. Note that changing
   * this while Duke is processing may have unpredictable
   * consequences.
   */
  public void setComparator(Comparator comparator);

  /**
   * Sets the high probability used for this property. Note that
   * changing this while Duke is processing may have unpredictable
   * consequences.
   */
  public void setHighProbability(double high);

  /**
   * Sets the low probability used for this property. Note that
   * changing this while Duke is processing may have unpredictable
   * consequences.
   */
  public void setLowProbability(double low);

  /**
   * Iff true the property should not be used for comparing records.
   */
  public boolean isIgnoreProperty();
  
  /**
   * Makes Duke skip this property when comparing records.
   */
  public void setIgnoreProperty(boolean ignore);

  /**
   * Sets the lookup behaviour of this property.
   */
  public void setLookupBehaviour(Lookup lookup);
  
  /**
   * Returns the probability that the records v1 and v2 represent the
   * same entity, based on high and low probability settings etc.
   */
  public double compare(String v1, String v2);

  /**
   * The lookup behaviour for this property.
   */
  public enum Lookup {
    // means: always look up this property, and require values to match
    REQUIRED,

    // always look up this property
    TRUE,

    // never look up this property
    FALSE,

    // default behaviour (look up if analysis says we should)
    DEFAULT
  }
}
