
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
import static junit.framework.Assert.fail;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkStatus;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.JDBCLinkDatabase;
import no.priv.garshol.duke.matchers.LinkDatabaseMatchListener;

public class LinkDatabaseMatchListenerTest {
  private LinkDatabase linkdb;
  private LinkDatabaseMatchListener listener;
  
  @Before
  public void setup() {
    List<Property> props = new ArrayList();
    props.add(new Property("id"));
    Configuration config = new Configuration();
    config.setProperties(props);
    config.setThreshold(0.45);
    linkdb = makeDatabase();
    if (linkdb instanceof JDBCLinkDatabase)
      // creates the schema automatically, if necessary
      ((JDBCLinkDatabase) linkdb).init(); 
    listener = new LinkDatabaseMatchListener(config, linkdb);
  }

  protected LinkDatabase makeDatabase() {
    return new JDBCLinkDatabase("org.h2.Driver", "jdbc:h2:mem:", "h2",
                                new Properties());
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
  public void testEmptyRecord() {
    Record r1 = makeRecord();
    Record r2 = makeRecord("id", "2");

    try {
      listener.startRecord(r1);
      listener.matches(r1, r2, 1.0);
      listener.endRecord();
      fail("accepted match with empty record");
    } catch (DukeException e) {
      // fails because we cannot capture a match with an empty record,
      // since it has no ID
    }
  }

  @Test
  public void testSingleRecord() {
    Record r1 = makeRecord("id", "1");
    Record r2 = makeRecord("id", "2");

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
    
    Record r1 = makeRecord("id", "1");

    listener.startRecord(r1);
    listener.endRecord();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    verifySame(new Link("1", "2", LinkStatus.RETRACTED, LinkKind.SAME),
               all.iterator().next());
  }

  @Test
  public void testSingleRecordPerhaps() {
    Record r1 = makeRecord("id", "1");
    Record r2 = makeRecord("id", "2");

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
    
    Record r1 = makeRecord("id", "1");
    Record r2 = makeRecord("id", "2");

    listener.startRecord(r1);
    listener.matches(r1, r2, 1.0);
    listener.endRecord();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    verifySame(new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME),
               all.iterator().next());
  }
    
  @Test
  public void testOverride() {
    Link l1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME);
    linkdb.assertLink(l1);
    
    Record r1 = makeRecord("id", "1");
    Record r2 = makeRecord("id", "2");

    listener.startRecord(r1);
    listener.matches(r1, r2, 1.0);
    listener.endRecord();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    verifySame(new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME),
               all.iterator().next());
  }
    
  @Test
  public void testOverride2() {
    Link l1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.DIFFERENT);
    linkdb.assertLink(l1);
    
    Record r1 = makeRecord("id", "1");
    Record r2 = makeRecord("id", "2");

    listener.startRecord(r1);
    listener.matches(r1, r2, 1.0);
    listener.endRecord();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    verifySame(new Link("1", "2", LinkStatus.ASSERTED, LinkKind.DIFFERENT),
               all.iterator().next());
  }
  
  public static void verifySame(Link l1, Link l2) {
    assertEquals("wrong ID1", l1.getID1(), l2.getID1());
    assertEquals("wrong ID2", l1.getID2(), l2.getID2());
    assertEquals("wrong status", l1.getStatus(), l2.getStatus());
    assertEquals("wrong kind", l1.getKind(), l2.getKind());
  }

  private void pause() {
    try {
      Thread.sleep(5); // ensure that timestamps are different
    } catch (InterruptedException e) {      
    }
  }

  private Record makeRecord() {
    return new RecordImpl(new HashMap());
  }
  
  private Record makeRecord(String prop, String val) {
    Map<String, Collection<String>> data = new HashMap();
    data.put(prop, Collections.singleton(val));
    return new RecordImpl(data);
  }
}
