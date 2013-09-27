
package no.priv.garshol.duke.genetic;

import java.util.List;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.fail;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import junit.framework.AssertionFailedError;

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.comparators.Levenshtein;

public class TestGeneticConfiguration {
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
    int differences = countDifferences(rand);
    assertTrue("Not enough differences: " + differences, differences > 3);
  }

  private int countDifferences(Configuration rand) {
    System.out.println("---------------------------------------------------------------------------");
    int differences = 0;
    if (rand.getThreshold() != config1.getThreshold())
      differences += 1;

    Property prop = rand.getPropertyByName("ID");
    assertTrue("ID property lost", prop.isIdProperty());

    differences += checkProperty("NAME", rand);
    differences += checkProperty("EMAIL", rand);
    System.out.println("DIFFERENCES: " + differences);
    return differences;
  }
  
  private int checkProperty(String name, Configuration rand) {
    Property prop = rand.getPropertyByName(name);
    Property orig = config1.getPropertyByName(name);

    int differences = 0;
    if (!prop.getComparator().equals(orig.getComparator())) {
      differences++;
      System.out.println("comparator different");
    }
    if (prop.getHighProbability() != orig.getHighProbability()); {
      differences++;
      System.out.println("high different, " + prop.getHighProbability() +
                         ", " + orig.getHighProbability());
    }
    if (prop.getLowProbability() != orig.getLowProbability()); {
      differences++;
      System.out.println("low different, " + prop.getLowProbability() +
                         ", " + orig.getLowProbability());
    }
    return differences;
  }

  //@Test
  public void testMutate() {
    GeneticConfiguration conf = new GeneticConfiguration(config1.copy());
    //conf.mutate();
    Configuration rand = conf.getConfiguration();

    assertEquals("wrong number of differences", 1, countDifferences(rand));
  }
}