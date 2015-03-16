
package no.priv.garshol.duke.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.comparators.Levenshtein;
import no.priv.garshol.duke.datasources.InMemoryDataSource;
import no.priv.garshol.duke.utils.DefaultRecordIterator;
import no.priv.garshol.duke.utils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DeduplicatorTest {
  private ConfigurationImpl config;
  private Processor processor;
  private TestUtils.TestListener listener;
  
  @Before
  public void setup() throws IOException {
    listener = new TestUtils.TestListener();
    Levenshtein comp = new Levenshtein();
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    props.add(new PropertyImpl("NAME", comp, 0.3, 0.8));
    props.add(new PropertyImpl("EMAIL", comp, 0.3, 0.8));

    config = new ConfigurationImpl();
    config.setProperties(props);
    config.setThreshold(0.85);
    config.setMaybeThreshold(0.8);
    processor = new Processor(config, true);
    processor.addMatchListener(listener);
  }

  @After
  public void cleanup() throws IOException {
    processor.close();
  }
  
  @Test
  public void testEmpty() throws IOException {
    processor.deduplicate(new ArrayList());
    assertEquals(0, listener.getMatches().size());
    assertEquals(0, listener.getRecordCount());
  }
  
  @Test
  public void testNoProperties() throws IOException {
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord());
    records.add(TestUtils.makeRecord());
    processor.deduplicate(records);
    assertEquals(0, listener.getMatches().size());
    assertEquals(2, listener.getRecordCount());
  }
  
  @Test
  public void testDoesNotMatch() throws IOException {
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1", "NAME", "A"));
    records.add(TestUtils.makeRecord("ID", "2", "NAME", "B"));
    processor.deduplicate(records);
    assertEquals(0, listener.getMatches().size());
    assertEquals(2, listener.getRecordCount());
  }
  
  @Test
  public void testDoesNotMatchEnough() throws IOException {
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1", "NAME", "A"));
    records.add(TestUtils.makeRecord("ID", "2", "NAME", "A"));
    processor.deduplicate(records);
    assertEquals(0, listener.getMatches().size());
    assertEquals(2, listener.getRecordCount());
  }
  
  @Test
  public void testMatches1() throws IOException {
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1", "NAME", "aaaaa", "EMAIL", "BBBBB"));
    records.add(TestUtils.makeRecord("ID", "2", "NAME", "aaaaa", "EMAIL", "BBBBB"));
    processor.deduplicate(records);
    
    assertEquals(2, listener.getRecordCount());
    Collection<TestUtils.Pair> matches = listener.getMatches();
    assertEquals(2, matches.size());
  }

  @Test
  public void testMatches2() throws IOException {
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1", "NAME", "AAAAA", "EMAIL", "BBBBB"));
    records.add(TestUtils.makeRecord("ID", "2", "NAME", "AAAAA", "EMAIL", "BBBBB"));
    processor.deduplicate(records);
    
    assertEquals(2, listener.getRecordCount());
    Collection<TestUtils.Pair> matches = listener.getMatches();
    assertEquals(2, matches.size());
  }
  
  @Test
  public void testLuceneKeyword() throws IOException {
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1", "NAME", "AND", "EMAIL", "BBBBB"));
    records.add(TestUtils.makeRecord("ID", "2", "NAME", "AND", "EMAIL", "BBBBB"));
    processor.deduplicate(records);
    
    assertEquals(2, listener.getRecordCount());
    Collection<TestUtils.Pair> matches = listener.getMatches();
    assertEquals(2, matches.size());
  }
  
  @Test
  public void testMultiToken() throws IOException {
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1", "NAME", "aaaaaaaaa aaaaa",
                                     "EMAIL", "bbbbb"));
    records.add(TestUtils.makeRecord("ID", "2", "NAME", "aaaaaaaaa aaaab",
                                     "EMAIL", "bbbbb"));
    processor.deduplicate(records);
    
    assertEquals(2, listener.getRecordCount());
    Collection<TestUtils.Pair> matches = listener.getMatches();
    assertEquals(2, matches.size());
  }

  @Test
  public void testMaybe() throws IOException {
    // this corresponds to maybe-threshold not being set at all
    config.setMaybeThreshold(0.0);

    // now lets try some matching
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1", "NAME", "aaaaaa",
                                     "EMAIL", "bbbbb"));
    records.add(TestUtils.makeRecord("ID", "2", "NAME", "bbbb",
                                     "EMAIL", "bbbbb"));
    processor.deduplicate(records);
    
    Collection<TestUtils.Pair> matches = listener.getMatches();
    // for (TestUtils.Pair match : matches)
    //   PrintMatchListener.show(match.r1, match.r2, match.conf, "MATCH");
    
    assertEquals("wrong number of records processed",
                 2, listener.getRecordCount());
    assertEquals("found matches, but shouldn't have",
                 0, matches.size());
    assertEquals("found maybe matches, but shouldn't have",
                 0, listener.getMaybeCount());
    assertEquals("wrong number of no-matches",
                 2, listener.getNoMatchCount());
  }
  
  @Test
  public void testNoComparator() throws IOException {
    // nulling out comparator
    config.getPropertyByName("EMAIL").setComparator(null);

    // now attempt to match
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1",
                                     "NAME", "aaaaa",
                                     "EMAIL", "BBBBB"));
    records.add(TestUtils.makeRecord("ID", "2",
                                     "NAME", "aaaaa",
                                     "EMAIL", "BBBBB"));
    processor.deduplicate(records);

    // this shouldn't produce any matches, because we're not comparing email
    assertEquals(0, listener.getMatches().size());
    assertEquals(2, listener.getRecordCount());
  }
  
  @Test
  public void testIgnoreProperty() throws IOException {
    // make email an ignored property
    Property prop = config.getPropertyByName("EMAIL");
    prop.setIgnoreProperty(true);

    // now run, and see that it doesn't match
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1", "NAME", "aaaaa", "EMAIL", "BBBBB"));
    records.add(TestUtils.makeRecord("ID", "2", "NAME", "aaaaa", "EMAIL", "BBBBB"));
    processor.deduplicate(records);

    // no matches found
    assertEquals(0, listener.getMatches().size());
  }

  @Test
  public void testBatchRemainder() throws IOException {
    // used to have a bug where batch_size = 1000, and having >1000 records
    // but <2000 would leave the records >1000 unprocessed

    // set up data source
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1",
                                     "NAME", "aaaaa",
                                     "EMAIL", "BBBBB"));
    records.add(TestUtils.makeRecord("ID", "2",
                                     "NAME", "aaaaa",
                                     "EMAIL", "BBBBB"));
    records.add(TestUtils.makeRecord("ID", "3",
                                     "NAME", "aaaaa",
                                     "EMAIL", "BBBBB"));
    TestDataSource source = new TestDataSource(records);
    config.addDataSource(0, source);

    // let's process!
    processor.deduplicate(2); // batch of 2, plus remaining one

    // so, what was the result?
    assertEquals("wrong number of matches", 4, listener.getMatches().size());
    assertEquals("wrong number of records processed",
                 3, listener.getRecordCount());
    assertEquals("wrong number of batches",
                 2, source.getBatchCount());
  }

  // ===== UTILITIES

  static class TestDataSource extends InMemoryDataSource {
    private int batch_count;

    public TestDataSource(Collection<Record> records) {
      super(records);
    }

    public RecordIterator getRecords() {
      return new TestRecordIterator(this, records.iterator());
    }

    public void batchProcessed() {
      batch_count++;
    }

    public int getBatchCount() {
      return batch_count;
    }
  }

  static class TestRecordIterator extends DefaultRecordIterator {
    private TestDataSource source;
    
    public TestRecordIterator(TestDataSource source, Iterator<Record> it) {
      super(it);
      this.source = source;
    }

    public void batchProcessed() {
      source.batchProcessed();
    }
  }
}