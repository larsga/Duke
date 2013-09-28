
package no.priv.garshol.duke.test;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import org.xml.sax.SAXException;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.ConfigWriter;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.comparators.Levenshtein;
  
public class ConfigWriterTest {
  @Rule
  public TemporaryFolder tmpdir = new TemporaryFolder();

  @Test
  public void testEmpty() throws IOException, SAXException {
    Configuration config = ConfigLoader.load("classpath:config-empty.xml");

    assertTrue(config.getDataSources().isEmpty());
    assertTrue(config.getDataSources(1).isEmpty());
    assertTrue(config.getDataSources(2).isEmpty());
    assertEquals(config.getPath(), null);
    assertEquals(config.getThreshold(), 0.4);
    assertEquals(config.getMaybeThreshold(), 0.0);
    assertTrue(config.getProperties().isEmpty());

    File outfile = tmpdir.newFile("config.xml");
    ConfigWriter.write(config, outfile.getAbsolutePath());
    config = ConfigLoader.load(outfile.getAbsolutePath());
    
    assertTrue(config.getDataSources().isEmpty());
    assertTrue(config.getDataSources(1).isEmpty());
    assertTrue(config.getDataSources(2).isEmpty());
    assertEquals(config.getPath(), null);
    assertEquals(config.getThreshold(), 0.4);
    assertEquals(config.getMaybeThreshold(), 0.0);
    assertTrue(config.getProperties().isEmpty());
  }

  @Test
  public void testDefaultProbs() throws IOException, SAXException {
    // --- build config
    Levenshtein lev = new Levenshtein();
    
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    props.add(new PropertyImpl("NAME", lev, 0.3, 0.8));
    props.add(new PropertyImpl("EMAIL", lev, 0.3, 0.8));
    
    Configuration config = new ConfigurationImpl();
    ((ConfigurationImpl) config).setProperties(props);
    ((ConfigurationImpl) config).setThreshold(0.85);
    ((ConfigurationImpl) config).setMaybeThreshold(0.7);

    // --- write and reload
    File outfile = tmpdir.newFile("config.xml");
    ConfigWriter.write(config, outfile.getAbsolutePath());
    config = ConfigLoader.load(outfile.getAbsolutePath());

    // --- verify loaded correctly    
    assertTrue(config.getDataSources().isEmpty());
    assertTrue(config.getDataSources(1).isEmpty());
    assertTrue(config.getDataSources(2).isEmpty());
    assertEquals(config.getPath(), null);
    assertEquals(config.getThreshold(), 0.85);
    assertEquals(config.getMaybeThreshold(), 0.7);
    assertEquals(3, config.getProperties().size());

    Property prop = config.getPropertyByName("ID");
    assertTrue("ID property lost", prop.isIdProperty());

    prop = config.getPropertyByName("NAME");
    assertEquals(lev.getClass(), prop.getComparator().getClass());
    assertEquals(0.3, prop.getLowProbability());
    assertEquals(0.8, prop.getHighProbability());

    prop = config.getPropertyByName("EMAIL");
    assertEquals(lev.getClass(), prop.getComparator().getClass());
    assertEquals(0.3, prop.getLowProbability());
    assertEquals(0.8, prop.getHighProbability());
  }
}