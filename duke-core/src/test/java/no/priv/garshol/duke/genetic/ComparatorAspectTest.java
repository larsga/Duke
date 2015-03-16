package no.priv.garshol.duke.genetic;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.PropertyImpl;

import org.junit.Before;
import org.junit.Test;

public class ComparatorAspectTest {
  private Configuration config1;
  private TestComparator comparator = new TestComparator();
  private String propName = "NAME";
  
  @Before
  public void setup() {
    
    config1 = new ConfigurationImpl();	
    
    List<Property> props = new ArrayList<Property>();
    props.add(new PropertyImpl("ID"));
    props.add(new PropertyImpl(propName, null, 0.3, 0.8));    
    
    ((ConfigurationImpl) config1).setProperties(props);
    ((ConfigurationImpl) config1).setThreshold(0.85);
  }

  @Test
  public void canAddCustomComparator() {
    GeneticConfiguration conf = new GeneticConfiguration(config1);	    
	Property aspectProp = new PropertyImpl(propName, null, 0.5, 0.5);
	List<Comparator> compList = new ArrayList<Comparator>();
	compList.add(comparator);
	ComparatorAspect aspect = new ComparatorAspect(aspectProp, compList);
	
	aspect.setRandomly(conf);
	
	Property updatedProp = config1.getPropertyByName(propName);
	Comparator randomComparator = updatedProp.getComparator();
    assertTrue("should have custom comparator set, but has : " + randomComparator.getClass(), randomComparator.equals(comparator));
  }
}

class TestComparator implements Comparator {

	@Override
	public boolean isTokenized() { 
		return false;
	}

	@Override
	public double compare(String v1, String v2) {		
		return 0;
	}
	
}