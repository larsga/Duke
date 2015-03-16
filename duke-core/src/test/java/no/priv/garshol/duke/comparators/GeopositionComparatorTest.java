
package no.priv.garshol.duke.comparators;

import no.priv.garshol.duke.DukeException;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class GeopositionComparatorTest {
  private GeopositionComparator comp;

  @Before
  public void setup() {
    comp = new GeopositionComparator();
  }
    
  @Test
  public void testEmpty() {
    assertEquals(0.5, comp.compare("", ""));
  }
    
  @Test
  public void testMalformed() {
    assertEquals(0.5, comp.compare("41.5,27.2", "41.5127.21"));
  }
    
  @Test
  public void testMalformed2() {
    assertEquals(0.5, comp.compare("41.5,27.2", "1231,123123"));
  }
    
  @Test
  public void testMalformedStrict() {
    comp.setStrict(true);
    try {
      assertEquals(0.5, comp.compare("41.5,27.2", "41.5127.21"));
      fail("Didn't catch bad value");
    } catch (DukeException e) {
      // success
    }
  }
    
  @Test
  public void testMalformed2Strict() {
    comp.setStrict(true);
    try {
      assertEquals(0.5, comp.compare("41.5,27.2", "1231,123123"));
      fail("Didn't catch bad value");
    } catch (DukeException e) {
      // success
    }
  }
    
  @Test
  public void testOsloKiev() {
    assertEquals(0.0, comp.compare("59.913869,10.752245", "50.45,30.5234"));
  }
    
  @Test
  public void testOsloKiev2() {
    String oslo = "59.913869,10.752245";
    String kiev = "50.45,30.5234";
    comp.setMaxDistance(2000 * 1000); // WolframAlpha gives distance as 1632km
    assertTrue(ratio(1550.0, 2000.0) > comp.compare(oslo, kiev));
    assertTrue(ratio(1700.0, 2000.0) < comp.compare(oslo, kiev));
  }

  private double ratio(double dist, double maxdist) {
    return ((1.0 - (dist / maxdist)) * 0.5 ) + 0.5;
  }
    
  @Test
  public void testOsloKiev3() {
    String oslo = "59.913869,10.752245";
    String kiev = "50.45,30.5234";
    comp.setMaxDistance(2000 * 1000); // WolframAlpha gives distance as 1632km
    assertEquals(comp.compare(oslo, kiev), comp.compare(kiev, oslo));
  }
}