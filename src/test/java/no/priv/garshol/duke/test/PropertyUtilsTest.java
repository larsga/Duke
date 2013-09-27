
package no.priv.garshol.duke.test;

import java.util.Properties;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.fail;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.utils.PropertyUtils;

public class PropertyUtilsTest {
  private Properties props;
  
  @Before
  public void setup() {
    props = new Properties();
    props.setProperty("foo", "bar");
    props.setProperty("baz", "2");
  }
  
  @Test
  public void testGet1() {
    assertEquals(PropertyUtils.get(props, "foo"), "bar");

    try {
      PropertyUtils.get(props, "bar");
      fail("exception not thrown");
    } catch (DukeConfigException e) {
    }
  }
  
  @Test
  public void testGet2() {
    assertEquals(PropertyUtils.get(props, "foo", "huhu"), "bar");
    assertEquals(PropertyUtils.get(props, "quux", "huhu"), "huhu");
  }
  
  @Test
  public void testGet3() {
    assertEquals(PropertyUtils.get(props, "baz", 0), 2);
    assertEquals(PropertyUtils.get(props, "quux", 27), 27);
  }
}