
package no.priv.garshol.duke.test;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;

import org.junit.Test;
import org.junit.Before;
import org.junit.Ignore;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.comparators.ExactComparator;

public class ConfigurationTest {

  @Test
  public void testTrivial() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    Property name = new PropertyImpl("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new PropertyImpl("EMAIL", comp, 0.3, 0.8);
    props.add(email);

    ConfigurationImpl config = new ConfigurationImpl();
    config.setThreshold(0.85);
    config.setProperties(props);

    Collection<Property> lookups = config.getLookupProperties();
    assertEquals(2, lookups.size());
    assertTrue(lookups.contains(name));
    assertTrue(lookups.contains(email));

    config.validate();
  }

  @Test
  public void testWithZeroes() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    Property name = new PropertyImpl("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new PropertyImpl("EMAIL", comp, 0.3, 0.8);
    props.add(email);
    props.add(new PropertyImpl("IGNORE", comp, 0.0, 0.0));

    ConfigurationImpl config = new ConfigurationImpl();
    config.setThreshold(0.85);
    config.setProperties(props);

    Collection<Property> lookups = config.getLookupProperties();
    assertEquals(2, lookups.size());
    assertTrue(lookups.contains(name));
    assertTrue(lookups.contains(email));

    config.validate();
  }

  @Test
  public void testJustOne() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    Property name = new PropertyImpl("NAME", comp, 0.0, 1.0);
    props.add(name);
    props.add(new PropertyImpl("EMAIL", comp, 0.0, 0.0));
    props.add(new PropertyImpl("IGNORE", comp, 0.0, 0.0));
    props.add(new PropertyImpl("IGNORE2", comp, 0.0, 0.0));

    ConfigurationImpl config = new ConfigurationImpl();
    config.setThreshold(0.85);
    config.setProperties(props);

    Collection<Property> lookups = config.getLookupProperties();
    assertEquals(lookups.size(), 1);
    assertTrue(lookups.contains(name));

    config.validate();
  }

  @Test
  public void testNoProperties() throws IOException {
    ConfigurationImpl config = new ConfigurationImpl();

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
    Property name = new PropertyImpl("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new PropertyImpl("EMAIL", comp, 0.3, 0.8);
    props.add(email);

    ConfigurationImpl config = new ConfigurationImpl();
    config.setThreshold(0.85);
    config.setProperties(props);

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
    props.add(new PropertyImpl("ID"));
    Property name = new PropertyImpl("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new PropertyImpl("EMAIL", comp, 0.3, 0.8);
    props.add(email);

    ConfigurationImpl config = new ConfigurationImpl();
    config.setThreshold(1.0);
    config.setProperties(props);

    try {
      config.validate();
      fail("Configuration which will never match anything accepted");
    } catch (DukeConfigException e) {
      // should fail, because cannot match any records, even if all
      // properties match 100%
    }
  }

  @Test
  public void testLookupProperties() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    Property name = new PropertyImpl("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new PropertyImpl("EMAIL", comp, 0.3, 0.8);
    props.add(email);

    ConfigurationImpl config = new ConfigurationImpl();
    config.setThreshold(0.85);
    config.setProperties(props);
    config.validate();

    Collection<Property> lookups = config.getLookupProperties();
    assertEquals(2, lookups.size());
    assertTrue(lookups.contains(name));
    assertTrue(lookups.contains(email));
  }

  @Test
  public void testLookupPropertiesDefault() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    Property name = new PropertyImpl("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new PropertyImpl("EMAIL", comp, 0.3, 0.8);
    email.setLookupBehaviour(Property.Lookup.DEFAULT);
    props.add(email);

    ConfigurationImpl config = new ConfigurationImpl();
    config.setThreshold(0.85);
    config.setProperties(props);
    config.validate();

    Collection<Property> lookups = config.getLookupProperties();
    assertEquals(2, lookups.size());
    assertTrue(lookups.contains(name));
    assertTrue(lookups.contains(email));
  }

  @Test
  public void testLookupPropertiesTurnedOn() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    Property name = new PropertyImpl("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new PropertyImpl("EMAIL", comp, 0.3, 0.8);
    props.add(email);
    Property phone = new PropertyImpl("PHONE", comp, 0.4, 0.51);
    props.add(phone);
    phone.setLookupBehaviour(Property.Lookup.TRUE);

    ConfigurationImpl config = new ConfigurationImpl();
    config.setThreshold(0.85);
    config.setProperties(props);
    config.validate();

    Collection<Property> lookups = config.getLookupProperties();
    assertEquals(3, lookups.size());
    assertTrue(lookups.contains(name));
    assertTrue(lookups.contains(email));
    assertTrue(lookups.contains(phone));
  }

  @Test
  public void testLookupPropertiesNotByDefault() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    Property name = new PropertyImpl("NAME", comp, 0.48, 0.8);
    props.add(name);
    Property email = new PropertyImpl("EMAIL", comp, 0.48, 0.8);
    props.add(email);
    Property phone = new PropertyImpl("PHONE", comp, 0.48, 0.51);
    props.add(phone);

    ConfigurationImpl config = new ConfigurationImpl();
    config.setThreshold(0.85);
    config.setProperties(props);
    config.validate();

    Collection<Property> lookups = config.getLookupProperties();
    assertEquals(2, lookups.size());
    assertTrue(lookups.contains(name));
    assertTrue(lookups.contains(email));
  }

  @Test
  public void testLookupPropertiesRequired() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    Property name = new PropertyImpl("NAME", comp, 0.3, 0.8);
    props.add(name);
    Property email = new PropertyImpl("EMAIL", comp, 0.3, 0.8);
    props.add(email);
    Property phone = new PropertyImpl("PHONE", comp, 0.4, 0.51);
    props.add(phone);
    phone.setLookupBehaviour(Property.Lookup.REQUIRED);

    ConfigurationImpl config = new ConfigurationImpl();
    config.setThreshold(0.85);
    config.setProperties(props);
    config.validate();

    Collection<Property> lookups = config.getLookupProperties();
    assertEquals(3, lookups.size());
    assertTrue(lookups.contains(name));
    assertTrue(lookups.contains(email));
    assertTrue(lookups.contains(phone));
  }

  @Test
  public void testCantHaveTwoIdProperties() throws IOException {
    ExactComparator comp = new ExactComparator();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID1"));
    props.add(new PropertyImpl("ID2"));
    props.add(new PropertyImpl("NAME", comp, 0.3, 0.86));

    ConfigurationImpl config = new ConfigurationImpl();
    config.setThreshold(0.85);
    config.setProperties(props);
    try {
      config.validate();
      fail("Can't have two ID properties in same config.");
    } catch (DukeConfigException e) {
      // this is the desired outcome
    }
  }
}
