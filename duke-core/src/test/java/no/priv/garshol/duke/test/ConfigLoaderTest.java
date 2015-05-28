
package no.priv.garshol.duke.test;

import java.io.IOException;
import java.util.List;

import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.databases.InMemoryDatabase;
import no.priv.garshol.duke.comparators.WeightedLevenshtein;
import no.priv.garshol.duke.comparators.WeightedLevenshtein.DefaultWeightEstimator;
import org.junit.Test;
import org.xml.sax.SAXException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class ConfigLoaderTest {

  @Test
  public void testEmpty() throws IOException, SAXException {
    Configuration config = ConfigLoader.load("classpath:config-empty.xml");

    assertTrue(config.getDataSources().isEmpty());
    assertTrue(config.getDataSources(1).isEmpty());
    assertTrue(config.getDataSources(2).isEmpty());
    assertEquals(config.getThreshold(), 0.4);
    assertEquals(config.getMaybeThreshold(), 0.0);
    assertTrue(config.getProperties().isEmpty());
  }

  @Test
  public void testString() throws IOException, SAXException {
    String cfg = "<duke>" +
        "<schema>" +
        "<threshold>0.4</threshold>" +
        "</schema>" +
        "</duke>";

    Configuration config = ConfigLoader.loadFromString(cfg);

    assertTrue(config.getDataSources().isEmpty());
    assertTrue(config.getDataSources(1).isEmpty());
    assertTrue(config.getDataSources(2).isEmpty());
    assertEquals(config.getThreshold(), 0.4);
    assertEquals(config.getMaybeThreshold(), 0.0);
    assertTrue(config.getProperties().isEmpty());
  }

  @Test
  public void testSingleGroup() throws IOException, SAXException {
    try {
      ConfigLoader.load("classpath:config-single-group.xml");
      fail("Config file with a single group was accepted");
    } catch (DukeConfigException e) {
      // this configuration is bad, so this is what we wanted to test
    }
  }

  @Test
  public void testDefaultProbs() throws IOException, SAXException {
    Configuration config = ConfigLoader.load("classpath:config-default-probs.xml");
    Property prop = config.getPropertyByName("FIRSTNAME");
    assertEquals(0.5, prop.getHighProbability());
    assertEquals(0.5, prop.getLowProbability());
    assertEquals(Property.Lookup.DEFAULT, prop.getLookupBehaviour());
  }

  @Test
  public void testDefaultComparator() throws IOException, SAXException {
    Configuration config = ConfigLoader.load("classpath:config-no-comparator.xml");
    Property prop = config.getPropertyByName("LASTNAME");
    assertEquals(null, prop.getComparator());
    assertEquals(Property.Lookup.DEFAULT, prop.getLookupBehaviour());
  }

  @Test
  public void testLookup() throws IOException, SAXException {
    Configuration config = ConfigLoader.load("classpath:config-lookup.xml");

    Property prop = config.getPropertyByName("FIRSTNAME");
    assertEquals(Property.Lookup.REQUIRED, prop.getLookupBehaviour());

    prop = config.getPropertyByName("LASTNAME");
    assertEquals(Property.Lookup.DEFAULT, prop.getLookupBehaviour());
  }

  @Test
  public void testParameterOfNothing() throws IOException, SAXException {
    try {
      ConfigLoader.load("classpath:config-no-object.xml");
      fail("Config file setting parameters of nothing was accepted");
    } catch (DukeConfigException e) {
      // this configuration is bad, so this is what we wanted to test
    }
  }

  @Test
  public void testCustomComparator() throws IOException, SAXException {
    Configuration config = ConfigLoader.load("classpath:config-custom-comparator.xml");

    List<Comparator> comparators = config.getCustomComparators();

    assertEquals(1, comparators.size());
  }

  @Test
  public void testCustomEstimator() throws IOException, SAXException {
    Configuration config = ConfigLoader.load("classpath:config-custom-estimator.xml");

    List<Comparator> comparators = config.getCustomComparators();
    assertEquals(1, comparators.size());

    WeightedLevenshtein wl = (WeightedLevenshtein) comparators.get(0);
    DefaultWeightEstimator est = (DefaultWeightEstimator) wl.getEstimator();
    assertEquals(3.8, est.getDigitWeight());
  }

  @Test
  public void testDatabaseDefault() throws IOException, SAXException {
    Configuration config = ConfigLoader.load("classpath:config-database.xml");
    Database db = config.getDatabase(false);
    assertTrue(db instanceof InMemoryDatabase);
  }
}
