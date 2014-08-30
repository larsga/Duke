
package no.priv.garshol.duke.genetic;

import java.util.List;
import java.util.ArrayList;

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.utils.ObjectUtils;

/**
 * A configuration created by the genetic algorithm.
 */
public class GeneticConfiguration implements Comparable<GeneticConfiguration> {
  private GeneticConfiguration parent;
  private Configuration config;
  private List<Aspect> aspects; // same collection on all config objects
  private double f;
  private int rank;

  // EVOLVABLE STRATEGY PARAMETERS
  // the evolutionary strategies research literature indicates that
  // the best way to set strategy parameters like mutation rate is to
  // let them evolve together with the actual configuration. we
  // therefore store them here.
  private int mutation_rate; // number of mutations per generation
  private double recombination_rate; // odds that we do recombination

  /**
   * Creates an initial copy of the starting configuration, with no
   * changes. Used to initialize the aspects list. Mutation and
   * recombination rates will self-evolve. Comparators will be
   * evolved.
   */
  public GeneticConfiguration(Configuration config) {
    this(config, -1, -1.0, true);
  }

  /**
   * Creates an initial copy of the starting configuration, with no
   * changes. Used to initialize the aspects list.
   * @param mutation_rate Mutation rate. -1 if self-evolving.
   * @param recombination_rate Recombination rate. -1.0 if self-evolving.
   * @param evolve_comparators If false the comparators will be left as
   *                           in the original configuration and not evolved.
   */
  public GeneticConfiguration(Configuration config, int mutation_rate,
                              double recombination_rate,
                              boolean evolve_comparators) {
	  
    this.config = config;
	List<Comparator> comparators = loadDefaultComparators();
	comparators.addAll(config.getCustomComparators());
    
    this.aspects = new ArrayList();
    aspects.add(new ThresholdAspect());
    for (Property prop : config.getProperties()) {
      if (!prop.isIdProperty()) {
        if (evolve_comparators)
          aspects.add(new ComparatorAspect(prop, comparators));
        aspects.add(new LowProbabilityAspect(prop));
        aspects.add(new HighProbabilityAspect(prop));
      }
    }
    if (mutation_rate == -1)
      aspects.add(new MutationRateAspect());
    else
      this.mutation_rate = mutation_rate;
    if (recombination_rate == -1.0)
      aspects.add(new RecombinationRateAspect());
    else
      this.recombination_rate = recombination_rate;
  }

  /**
   * Creates a copy of the starting configuration, keeping the aspects
   * list.
   */
  public GeneticConfiguration(GeneticConfiguration config) {
    this.parent = config;
    this.config = parent.getConfiguration().copy();
    this.aspects = parent.aspects;
    this.mutation_rate = config.getMutationRate();
    this.recombination_rate = config.getRecombinationRate();
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
    for (Aspect aspect : aspects)
      aspect.setRandomly(copy);
    return copy;
  }

  /**
   * Returns a copy of the configuration where the strategy parameters
   * are randomized, but the rest is untouched.
   */
  public GeneticConfiguration makeCopy() {
    GeneticConfiguration copy = new GeneticConfiguration(this);
    for (Aspect aspect : aspects)
      if (aspect instanceof RecombinationRateAspect ||
          aspect instanceof MutationRateAspect)
        aspect.setRandomly(copy);
    return copy;
  }

  /**
   * The mutation rate of this individual.
   */
  public int getMutationRate() {
    return mutation_rate;
  }

  /**
   * Sets the mutation rate of this individual.
   */
  public void setMutationRate(int mutation_rate) {
    this.mutation_rate = mutation_rate;
  }

  /**
   * The recombination rate of this individual.
   */
  public double getRecombinationRate() {
    return recombination_rate;
  }

  /**
   * Makes one random change to the configuration.
   */
  public void mutate() {
    Aspect aspect = aspects.get((int) (Math.random() * aspects.size()));
    aspect.setRandomly(this);
  }

  /**
   * Mates this configuration with another configuration.  Randomly
   * sets half the aspects of this configuration to those we already
   * have, and the other half to those of the other configuration.
   */
  public void mateWith(GeneticConfiguration other) {
    for (Aspect aspect : aspects)
      if (Math.random() < 0.5)
        aspect.setFromOther(this, other);
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

    buf.append(" mr=" + mutation_rate +
               " rr=" + shortnum(recombination_rate));

    buf.append("]");
    return buf.toString();
  }

  private String shortname(Comparator comp) {
    return comp.getClass().getSimpleName();
  }

  static String shortnum(double number) {
    String str = "" + number;
    if (str.length() > 4)
      return str.substring(0, 4);
    else
      return str;
  }

  private List<Comparator> loadDefaultComparators() {
	  String PKG = "no.priv.garshol.duke.comparators.";
	  String[] compnames = new String[] {
			  "DiceCoefficientComparator",
			  "DifferentComparator",
			  "ExactComparator",
			  "JaroWinkler",
			  "JaroWinklerTokenized",
			  "Levenshtein",
			  "NumericComparator",
			  "PersonNameComparator",
			  "SoundexComparator",
			  "WeightedLevenshtein",
			  "NorphoneComparator",
			  "MetaphoneComparator",
			  "QGramComparator",
			  "GeopositionComparator",
			  "LongestCommonSubstring",
	  };

	  List<Comparator> comparators = new ArrayList<Comparator>();
	  for (int ix = 0; ix < compnames.length; ix++)
		  comparators.add((Comparator)ObjectUtils.instantiate(PKG + compnames[ix]));
	  
	  return comparators;  
  }  

  // ----- ASPECTS for strategy parameters

  static class MutationRateAspect extends Aspect {
    public void setRandomly(GeneticConfiguration config) {
      // cannot allow this to be zero, since that freezes all development,
      // and effectively leaves us stuck where we are
      config.mutation_rate = 1 + (int) (Math.random() * 10);
    }

    public void setFromOther(GeneticConfiguration config,
                             GeneticConfiguration other) {
      config.mutation_rate = other.mutation_rate;
    }
  }

  static class RecombinationRateAspect extends Aspect {
    public void setRandomly(GeneticConfiguration config) {
      // a ceiling of 5 is arbitrary. appears to work well in practice
      config.recombination_rate = Math.random() * 5;
    }

    public void setFromOther(GeneticConfiguration config,
                             GeneticConfiguration other) {
      config.recombination_rate = other.recombination_rate;
    }
  }
}
