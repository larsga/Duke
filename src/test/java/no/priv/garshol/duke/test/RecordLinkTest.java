
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import junit.framework.AssertionFailedError;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.matchers.AbstractMatchListener;
import no.priv.garshol.duke.comparators.Levenshtein;
import no.priv.garshol.duke.datasources.InMemoryDataSource;

public class RecordLinkTest {
  private Configuration config;
  private Processor processor;
  private InMemoryDataSource source1;
  private InMemoryDataSource source2;
  private TestUtils.TestListener listener;
  
  @Before
  public void setup() throws CorruptIndexException, IOException {
    listener = new TestUtils.TestListener();
    Levenshtein comp = new Levenshtein();
    List<Property> props = new ArrayList();
    props.add(new Property("ID"));
    props.add(new Property("NAME", comp, 0.3, 0.8));
    props.add(new Property("EMAIL", comp, 0.3, 0.8));

    config = new Configuration();
    config.setProperties(props);
    config.setThreshold(0.85);
    config.setMaybeThreshold(0.8);

    source1 = new InMemoryDataSource();
    source2 = new InMemoryDataSource();
    config.addDataSource(1, source1);
    config.addDataSource(2, source2);
    
    processor = new Processor(config, true);
    processor.addMatchListener(listener);
  }

  @After
  public void cleanup() throws CorruptIndexException, IOException {
    processor.close();
  }
  
  @Test
  public void testEmpty() throws IOException {
    processor.link();
    assertEquals(0, listener.getMatches().size());
    assertEquals(0, listener.getRecordCount());
  }

  @Test
  public void testSimplePair() throws IOException {
    source1.add(TestUtils.makeRecord("ID", "1", "NAME", "aaaaa", "EMAIL", "bbbbb"));
    source2.add(TestUtils.makeRecord("ID", "2", "NAME", "aaaaa", "EMAIL", "bbbbb"));
    processor.link();

    assertEquals("bad record count", 1, listener.getRecordCount());
    List<TestUtils.Pair> matches = listener.getMatches();
    assertEquals("bad number of matches", 1, matches.size());

    TestUtils.Pair pair = matches.get(0);
    if (pair.r1.getValue("ID").equals("2")) {
      Record r = pair.r1;
      pair.r1 = pair.r2;
      pair.r2 = r;
    }

    assertEquals("1", pair.r1.getValue("ID"));
    assertEquals("2", pair.r2.getValue("ID"));
  }

  @Test
  public void testOneMatchOneMiss() throws IOException {
    source1.add(TestUtils.makeRecord("ID", "1", "NAME", "aaaaa", "EMAIL", "bbbbb"));
    source2.add(TestUtils.makeRecord("ID", "2", "NAME", "aaaaa", "EMAIL", "bbbbb"));
    source2.add(TestUtils.makeRecord("ID", "3", "NAME", "xxxx", "EMAIL", "yyyyy"));
    processor.link();

    assertEquals("bad record count", 2, listener.getRecordCount());
    List<TestUtils.Pair> matches = listener.getMatches();
    assertEquals("bad number of matches", 1, matches.size());
    assertEquals("bad number of missed matches", 1, listener.getNoMatchCount());

    TestUtils.Pair pair = matches.get(0);
    if (pair.r1.getValue("ID").equals("2")) {
      Record r = pair.r1;
      pair.r1 = pair.r2;
      pair.r2 = r;
    }

    assertEquals("1", pair.r1.getValue("ID"));
    assertEquals("2", pair.r2.getValue("ID"));
  }

  @Test
  public void testOneMatchOneMiss2() throws IOException {
    config.setMaybeThreshold(0.0);
    source1.add(TestUtils.makeRecord("ID", "1", "NAME", "aaaaa", "EMAIL", "bbbbb"));
    source2.add(TestUtils.makeRecord("ID", "2", "NAME", "aaaaa", "EMAIL", "bbbbb"));
    source2.add(TestUtils.makeRecord("ID", "3", "NAME", "xxxxx", "EMAIL", "bbbbb"));
    processor.link();

    assertEquals("bad record count", 2, listener.getRecordCount());
    List<TestUtils.Pair> matches = listener.getMatches();
    assertEquals("bad number of matches", 1, matches.size());
    assertEquals("bad number of missed matches", 1, listener.getNoMatchCount());

    TestUtils.Pair pair = matches.get(0);
    if (pair.r1.getValue("ID").equals("2")) {
      Record r = pair.r1;
      pair.r1 = pair.r2;
      pair.r2 = r;
    }

    assertEquals("1", pair.r1.getValue("ID"));
    assertEquals("2", pair.r2.getValue("ID"));
  }
  
}