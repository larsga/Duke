
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
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.comparators.Levenshtein;
import no.priv.garshol.duke.matchers.AbstractMatchListener;

public class DeduplicatorTest {
  private Processor processor;
  private TestUtils.TestListener listener;
  
  @Before
  public void setup() throws CorruptIndexException, IOException {
    listener = new TestUtils.TestListener();
    Levenshtein comp = new Levenshtein();
    List<Property> props = new ArrayList();
    props.add(new Property("ID"));
    props.add(new Property("NAME", comp, 0.3, 0.8));
    props.add(new Property("EMAIL", comp, 0.3, 0.8));

    Configuration config = new Configuration();
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

  @Test @Ignore
  public void testMatches2() throws IOException {
    Collection<Record> records = new ArrayList();
    records.add(TestUtils.makeRecord("ID", "1", "NAME", "AAAAA", "EMAIL", "BBBBB"));
    records.add(TestUtils.makeRecord("ID", "2", "NAME", "AAAAA", "EMAIL", "BBBBB"));
    processor.deduplicate(records);
    
    assertEquals(2, listener.getRecordCount());
    Collection<TestUtils.Pair> matches = listener.getMatches();
    assertEquals(2, matches.size());
  }
  
  @Test @Ignore
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
}