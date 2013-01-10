
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

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.comparators.ExactComparator;

public class ConfigurationTest {
  
  @Test
  public void testTrivial() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new Property("ID"));
    Property name = new Property("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new Property("EMAIL", comp, 0.3, 0.8);
    props.add(email);

    Configuration config = new Configuration();
    config.setProperties(props);
    config.setThreshold(0.85);

    Collection<Property> lookups = config.getLookupProperties();
    assertEquals(lookups.size(), 2);
    assertTrue(lookups.contains(name));
    assertTrue(lookups.contains(email));

    config.validate();
  }
  
  @Test
  public void testWithZeroes() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new Property("ID"));
    Property name = new Property("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new Property("EMAIL", comp, 0.3, 0.8);
    props.add(email);
    props.add(new Property("IGNORE", comp, 0.0, 0.0));

    Configuration config = new Configuration();
    config.setProperties(props);
    config.setThreshold(0.85);

    Collection<Property> lookups = config.getLookupProperties();
    assertEquals(lookups.size(), 2);
    assertTrue(lookups.contains(name));
    assertTrue(lookups.contains(email));

    config.validate();
  }
  
  @Test
  public void testJustOne() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new Property("ID"));
    Property name = new Property("NAME", comp, 0.0, 1.0);
    props.add(name);
    props.add(new Property("EMAIL", comp, 0.0, 0.0));
    props.add(new Property("IGNORE", comp, 0.0, 0.0));
    props.add(new Property("IGNORE2", comp, 0.0, 0.0));

    Configuration config = new Configuration();
    config.setProperties(props);
    config.setThreshold(0.85);

    Collection<Property> lookups = config.getLookupProperties();
    assertEquals(lookups.size(), 1);
    assertTrue(lookups.contains(name));

    config.validate();
  }

  @Test
  public void testNoProperties() throws IOException {
    Configuration config = new Configuration();

    try {
      config.validate();
      fail("Configuration with no properties accepted");
    } catch (DukeConfigException e) {
      // yep, should fail, because it doesn't have *any* properties
    }
  }
  
  @Test
  public void testNoIdProperties() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    Property name = new Property("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new Property("EMAIL", comp, 0.3, 0.8);
    props.add(email);

    Configuration config = new Configuration();
    config.setProperties(props);
    config.setThreshold(0.85);

    try {
      config.validate();
      fail("Configuration with no ID properties accepted");
    } catch (DukeConfigException e) {
      // yep, should fail, due to lack of ID property
    }
  }
  
  @Test
  public void testThresholdTooHigh() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new Property("ID"));
    Property name = new Property("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new Property("EMAIL", comp, 0.3, 0.8);
    props.add(email);

    Configuration config = new Configuration();
    config.setProperties(props);
    config.setThreshold(1.0);

    try {
      config.validate();
      fail("Configuration which will never match anything accepted");
    } catch (DukeConfigException e) {
      // should fail, because cannot match any records, even if all
      // properties match 100%
    }
  }
}