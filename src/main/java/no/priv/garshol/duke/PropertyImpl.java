
package no.priv.garshol.duke;

/**
 * The default implementation of the Property interface.
 */
public class PropertyImpl implements Property {
  private String name;
  private boolean id;
  private boolean analyzed;      // irrelevant if ID
  private boolean ignore;        // irrelevant if ID
  private Comparator comparator; // irrelevant if ID
  private double high;           // irrelevant if ID
  private double low;            // irrelevant if ID
  private Lookup lookup;         // irrelevant if ID

  // used to initialize ID properties
  public PropertyImpl(String name) {
    this.name = name;
    this.id = true;
    this.analyzed = false;
    this.lookup = Lookup.FALSE;
  }

  public PropertyImpl(String name, Comparator comparator, double low,
                      double high) {
    this.name = name;
    this.id = false;
    this.analyzed = comparator != null && comparator.isTokenized();
    this.comparator = comparator;
    this.high = high;
    this.low = low;
    this.lookup = Lookup.DEFAULT;
  }

  // FIXME: rules for property names?
  public String getName() {
    return name;
  }

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

  public Lookup getLookupBehaviour() {
    return lookup;
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
   * Sets the lookup behaviour of this property.
   */
  public void setLookupBehaviour(Lookup lookup) {
    this.lookup = lookup;
  }

  /**
   * Returns the probability that the records v1 and v2 came from
   * represent the same entity, based on high and low probability
   * settings etc.
   */
  public double compare(String v1, String v2) {
    // FIXME: it should be possible here to say that, actually, we
    // didn't learn anything from comparing these two values, so that
    // probability is set to 0.5.

    if (comparator == null)
      return 0.5; // we ignore properties with no comparator

    // first, we call the comparator, to get a measure of how similar
    // these two values are. note that this is not the same as what we
    // are going to return, which is a probability.
    double sim = comparator.compare(v1, v2);

    // we have been configured with a high probability (for equal
    // values) and a low probability (for different values). given
    // sim, which is a measure of the similarity somewhere in between
    // equal and different, we now compute our estimate of the
    // probability.

    // if sim = 1.0, we return high. if sim = 0.0, we return low. for
    // values in between we need to compute a little.  the obvious
    // formula to use would be (sim * (high - low)) + low, which
    // spreads the values out equally spaced between high and low.

    // however, if the similarity is higher than 0.5 we don't want to
    // consider this negative evidence, and so there's a threshold
    // there.  also, users felt Duke was too eager to merge records,
    // and wanted probabilities to fall off faster with lower
    // probabilities, and so we square sim in order to achieve this.

    if (sim >= 0.5)
      return ((high - 0.5) * (sim * sim)) + 0.5;
    else
      return low;
  }

  public Property copy() {
    if (id)
      return new PropertyImpl(name);

    PropertyImpl p = new PropertyImpl(name, comparator, low, high);
    p.setIgnoreProperty(ignore);
    p.setLookupBehaviour(lookup);
    return p;
  }

  public String toString() {
    return "[Property " + name + "]";
  }
}
