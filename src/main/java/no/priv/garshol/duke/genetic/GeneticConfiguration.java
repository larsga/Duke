
package no.priv.garshol.duke.genetic;

import java.util.List;
import java.util.ArrayList;

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.Configuration;

/**
 * A configuration created by the genetic algorithm.
 */
public class GeneticConfiguration implements Comparable<GeneticConfiguration> {
  private GeneticConfiguration parent;
  private Configuration config;
  private List<Aspect> aspects; // same collection on all config objects
  private double f;
  private int rank;

  /**
   * Creates an initial copy of the starting configuration, with no
   * changes. Used to initialize the aspects list.
   */
  public GeneticConfiguration(Configuration config) {
    this.config = config;
    this.aspects = new ArrayList();
    aspects.add(new ThresholdAspect());
    for (Property prop : config.getProperties()) {
      if (!prop.isIdProperty()) {
        aspects.add(new ComparatorAspect(prop));
        aspects.add(new LowProbabilityAspect(prop));
        aspects.add(new HighProbabilityAspect(prop));
      }
    }
  }

  /**
   * Creates a copy of the starting configuration, keeping the aspects
   * list.
   */
  public GeneticConfiguration(GeneticConfiguration config) {
    this.parent = config;
    this.config = parent.getConfiguration().copy();
    this.aspects = parent.aspects;
  }
  
  /**
   * Returns the underlying Duke configuration.
   */
  public Configuration getConfiguration() {
    return config;
  }

  /**
   * Returns the ranking of this configuration within its generation.
   * 1 means it was the best.
   */
  public int getRank() {
    return rank;
  }

  /**
   * Sets the rank of this configuration within its generation.
   */
  public void setRank(int rank) {
    this.rank = rank;
  }

  /**
   * Returns the F-score of this configuration.
   */
  public double getFNumber() {
    return f;
  }

  /**
   * Sets the F-score of this configuration.
   */
  public void setFNumber(double f) {
    this.f = f;
  }

  /**
   * Returns the configuration this configuration was derived from, if
   * any.
   */
  public GeneticConfiguration getParent() {
    return parent;
  }

  /**
   * Returns a randomized copy of the configuration.
   */
  public GeneticConfiguration makeRandomCopy() {
    GeneticConfiguration copy = new GeneticConfiguration(this);
    Configuration theconfig = copy.getConfiguration();
    for (Aspect aspect : aspects)
      aspect.setRandomly(theconfig);
    return copy;
  }

  /**
   * Makes one random change to the configuration.
   */
  public void mutate() {
    Aspect aspect = aspects.get((int) (Math.random() * aspects.size()));
    aspect.setRandomly(config);
  }

  /**
   * Mates this configuration with another configuration.  Randomly
   * sets half the aspects of this configuration to those we already
   * have, and the other half to those of the other configuration.
   */
  public void mateWith(GeneticConfiguration other) {
    Configuration otherc = other.getConfiguration();
    for (Aspect aspect : aspects)
      if (Math.random() < 0.5)
        aspect.setFromOther(config, otherc);
      // else keep our own
  }

  public int compareTo(GeneticConfiguration other) {
    if (f < other.getFNumber())
      return 1;
    else if (f == other.getFNumber())
      return 0;
    else
      return -1;
  }

  /**
   * Returns the brief summary used in the command-line output.
   */
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("[GeneticConfiguration " + shortnum(config.getThreshold()));
    for (Property p : config.getProperties())
      if (p.isIdProperty())
        buf.append(" [" + p.getName() + "]");
      else
        buf.append(" [" + p.getName() + " " + shortname(p.getComparator()) +
                   " " + shortnum(p.getHighProbability()) + " " +
                   shortnum(p.getLowProbability()) +
                   "]");

    buf.append("]");
    return buf.toString();
  }

  private String shortname(Comparator comp) {
    return comp.getClass().getSimpleName();
  }
  
  private String shortnum(double number) {
    String str = "" + number;
    if (str.length() > 4)
      return str.substring(0, 4);
    else
      return str;
  }
}