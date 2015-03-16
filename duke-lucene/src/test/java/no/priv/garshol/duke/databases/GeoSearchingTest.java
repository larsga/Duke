
package no.priv.garshol.duke.databases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.comparators.GeopositionComparator;
import no.priv.garshol.duke.utils.TestUtils;
import org.apache.lucene.index.CorruptIndexException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

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
    // Define database as LuceneDatabase (not default anymore)
    config.addDatabase(new LuceneDatabase());
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
  
  @Test
  public void testBadCoordinate() throws IOException {
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1",
                                     "LOCATION", "59.948011,11.042239"));
    records.add(TestUtils.makeRecord("ID", "2",
                                     "LOCATION", "159.948053,11.042276"));

    try {
      processor.deduplicate(records);
    } catch (com.spatial4j.core.exception.InvalidShapeException e) {
      // this is not a legal coordinate, because a latitude of 159 degrees
      // makes no sense
      return;
    }

    fail("Invalid coordinate accepted.");
  }
}