
package no.priv.garshol.duke.test;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;

import org.junit.Test;
import org.junit.Before;
import org.junit.Ignore;
import static junit.framework.Assert.fail;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import org.xml.sax.SAXException;

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.DukeConfigException;
  
public class ConfigLoaderTest {

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
}