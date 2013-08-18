
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import junit.framework.AssertionFailedError;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.comparators.GeopositionComparator;
import no.priv.garshol.duke.matchers.AbstractMatchListener;
import no.priv.garshol.duke.matchers.PrintMatchListener;

public class GeoSearchingTest {
  private ConfigurationImpl config;
  private Processor processor;
  private TestUtils.TestListener listener;
  
  @Before
  public void setup() throws CorruptIndexException, IOException {
    listener = new TestUtils.TestListener();
    List<Property> props = new ArrayList();
    GeopositionComparator comp = new GeopositionComparator();
    comp.setMaxDistance(100);
    props.add(new PropertyImpl("ID"));
    props.add(new PropertyImpl("LOCATION", comp, 0.3, 0.9));

    config = new ConfigurationImpl();
    config.setProperties(props);
    config.setThreshold(0.85);
    config.setMaybeThreshold(0.8);
    processor = new Processor(config, true);
    processor.addMatchListener(listener);
  }

  @After
  public void cleanup() throws CorruptIndexException, IOException {
    processor.close();
  }
  
  @Test
  public void testGeosearch() throws IOException {
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1",
                                     "LOCATION", "59.948011,11.042239"));
    records.add(TestUtils.makeRecord("ID", "2",
                                     "LOCATION", "59.948053,11.042276"));
    processor.deduplicate(records);

    // should find 2 matches
    assertEquals(2, listener.getMatches().size());
    assertEquals(2, listener.getRecordCount());
  }
}