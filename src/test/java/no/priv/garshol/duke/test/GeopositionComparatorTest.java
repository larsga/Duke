
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.comparators.GeopositionComparator;

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