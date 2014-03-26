
package no.priv.garshol.duke.genetic;

import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.comparators.Levenshtein;
import no.priv.garshol.duke.comparators.WeightedLevenshtein;

public class GeneticConfigurationTest {
  private Configuration config1;

  @Before
  public void setup() {
    Levenshtein lev = new Levenshtein();
    
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    props.add(new PropertyImpl("NAME", lev, 0.3, 0.8));
    props.add(new PropertyImpl("EMAIL", lev, 0.3, 0.8));
    
    config1 = new ConfigurationImpl();
    ((ConfigurationImpl) config1).setProperties(props);
    ((ConfigurationImpl) config1).setThreshold(0.85);
  }

  @Test
  public void testMakeRandomCopy() {
    GeneticConfiguration conf = new GeneticConfiguration(config1);
    GeneticConfiguration confrand = conf.makeRandomCopy();
    Configuration rand = confrand.getConfiguration();

    assertTrue("shouldn't have a parent", conf.getParent() == null);
    assertTrue("wrong parent", confrand.getParent() == conf);
    assertEquals("wrong number of properties",
                 rand.getProperties().size(), 3);
    
    // same properties, but most aspects should now be different.
    // don't really want to a computation of how many aspects are different,
    // because with some degree of statistical likelihood, any limit is going
    // to be wrong some of the time. let's try this one and see how it works
    // out. let me know if this causes problems.
    int aspects = 1 + (3 * (config1.getProperties().size() - 1));
    int differences = countDifferences(config1, rand);
    assertTrue("Not enough differences: " + differences, differences > 3);
  }

  private int countDifferences(Configuration config, Configuration rand) {
    int differences = 0;
    if (rand.getThreshold() != config.getThreshold())
      differences += 1;

    Property prop = rand.getPropertyByName("ID");
    assertTrue("ID property lost", prop.isIdProperty());

    differences += checkProperty("NAME", config, rand);
    differences += checkProperty("EMAIL", config, rand);
    return differences;
  }
  
  private int checkProperty(String name, Configuration config,
                            Configuration rand) {
    Property prop = rand.getPropertyByName(name);
    Property orig = config.getPropertyByName(name);

    int differences = 0;
    if (!prop.getComparator().equals(orig.getComparator()))
      differences++;
    if (prop.getHighProbability() != orig.getHighProbability())
      differences++;
    if (prop.getLowProbability() != orig.getLowProbability())
      differences++;
    return differences;
  }

  @Test
  public void testMutate() {
    GeneticConfiguration conf = new GeneticConfiguration(config1.copy());
    conf.mutate();
    Configuration rand = conf.getConfiguration();

    int diffs = countDifferences(config1, rand);
    if (diffs == 0) {
      // this happens every now and then by accident. when it does, we
      // give it a second try.
      conf.mutate();
      diffs = countDifferences(config1, rand);
      if (diffs == 0) {
        // ok, third try
        conf.mutate();
        diffs = countDifferences(config1, rand);
      }
      // of course, it could still fail, but at least the chance is
      // greatly reduced now
    }
        
    assertEquals("wrong number of differences", 1, diffs);
  }

  @Test
  public void testMate() {
    // build a different configuration
    WeightedLevenshtein lev = new WeightedLevenshtein();
    
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    props.add(new PropertyImpl("NAME", lev, 0.2, 0.9));
    props.add(new PropertyImpl("EMAIL", lev, 0.2, 0.9));
    
    Configuration other = new ConfigurationImpl();
    ((ConfigurationImpl) other).setProperties(props);
    ((ConfigurationImpl) other).setThreshold(0.75);
    GeneticConfiguration g_other = new GeneticConfiguration(other);

    // proceed to mate
    GeneticConfiguration conf = new GeneticConfiguration(config1.copy());
    conf.mateWith(g_other);
    Configuration rand = conf.getConfiguration();

    // compute differences
    // there are seven aspects, which should always be equal to just one
    // of the original configurations. comparing against both should therefore
    // always yield exactly 7 differences.
    assertEquals("wrong number of differences", 7,
                 countDifferences(config1, rand) +
                 countDifferences(other, rand));
  }
}
