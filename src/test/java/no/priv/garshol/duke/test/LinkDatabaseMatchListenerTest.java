
package no.priv.garshol.duke.test;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkStatus;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.JDBCLinkDatabase;
import no.priv.garshol.duke.LinkDatabaseMatchListener;

public class LinkDatabaseMatchListenerTest {
  private JDBCLinkDatabase linkdb;
  private LinkDatabaseMatchListener listener;
  
  @Before
  public void setup() {
    List<Property> props = new ArrayList();
    props.add(new Property("id"));
    Configuration config = new Configuration();
    config.setProperties(props);
    config.setThreshold(0.45);
    linkdb = new JDBCLinkDatabase("org.h2.Driver",
                                  "jdbc:h2:test",
                                  "h2",
                                  new Properties());
    // linkdb creates the schema automatically, if necessary
    listener = new LinkDatabaseMatchListener(config, linkdb);
  }

  @After
  public void cleanup() {
    linkdb.clear();
    linkdb.close();
  }
  
  @Test
  public void testEmpty() {
    // nothing's happened, so there should be no links
    assertTrue(linkdb.getAllLinks().isEmpty());
  }

  @Test
  public void testSingleRecord() {
    Map<String, Collection<String>> data = new HashMap();
    data.put("id", Collections.singleton("1"));
    Record r1 = new RecordImpl(data);

    data = new HashMap();
    data.put("id", Collections.singleton("2"));
    Record r2 = new RecordImpl(data);    

    listener.startRecord(r1);
    listener.matches(r1, r2, 1.0);
    listener.endRecord();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    verifySame(new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME),
               all.iterator().next());
  }

  @Test
  public void testSingleRecordRetract() {
    testSingleRecord(); // now we've asserted they're equal. then let's retract
    pause(); // ensure timestamps are different
    
    Map<String, Collection<String>> data = new HashMap();
    data.put("id", Collections.singleton("1"));
    Record r1 = new RecordImpl(data);

    listener.startRecord(r1);
    listener.endRecord();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    verifySame(new Link("1", "2", LinkStatus.RETRACTED, LinkKind.SAME),
               all.iterator().next());
  }

  @Test
  public void testSingleRecordPerhaps() {
    Map<String, Collection<String>> data = new HashMap();
    data.put("id", Collections.singleton("1"));
    Record r1 = new RecordImpl(data);

    data = new HashMap();
    data.put("id", Collections.singleton("2"));
    Record r2 = new RecordImpl(data);    

    listener.startRecord(r1);
    listener.matchesPerhaps(r1, r2, 1.0);
    listener.endRecord();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    verifySame(new Link("1", "2", LinkStatus.INFERRED, LinkKind.MAYBESAME),
               all.iterator().next());
  }

  @Test
  public void testUpgradeFromPerhaps() {
    testSingleRecordPerhaps();
    pause(); // ensure timestamps are different
    
    Map<String, Collection<String>> data = new HashMap();
    data.put("id", Collections.singleton("1"));
    Record r1 = new RecordImpl(data);

    data = new HashMap();
    data.put("id", Collections.singleton("2"));
    Record r2 = new RecordImpl(data);    

    listener.startRecord(r1);
    listener.matches(r1, r2, 1.0);
    listener.endRecord();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    verifySame(new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME),
               all.iterator().next());
  }
  
  private void verifySame(Link l1, Link l2) {
    assertEquals(l1.getID1(), l2.getID1());
    assertEquals(l1.getID2(), l2.getID2());
    assertEquals(l1.getStatus(), l2.getStatus());
    assertEquals(l1.getKind(), l2.getKind());
  }

  private void pause() {
    try {
      Thread.sleep(5); // ensure that timestamps are different
    } catch (InterruptedException e) {      
    }
  }
}
