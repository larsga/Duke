
package no.priv.garshol.duke.matchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import no.priv.garshol.duke.*;
import no.priv.garshol.duke.utils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LinkDatabaseMatchListenerTest {
  private LinkDatabase linkdb;
  private LinkDatabaseMatchListener listener;

  @Before
  public void setup() {
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("id"));
    ConfigurationImpl config = new ConfigurationImpl();
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
      listener.startProcessing();
      listener.batchReady(1);
      listener.matches(r1, r2, 1.0);
      listener.batchDone();
      listener.endProcessing();
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

    listener.startProcessing();
    listener.batchReady(1);
    listener.matches(r1, r2, 0.95);
    listener.batchDone();
    listener.endProcessing();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    TestUtils.verifySame(new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME, 0.95),
        all.iterator().next());
  }

  @Test
  public void testSingleRecordRetract() {
    testSingleRecord(); // now we've asserted they're equal. then let's retract
    pause(); // ensure timestamps are different

    Record r1 = makeRecord("id", "1");

    listener.startProcessing();
    listener.batchReady(0);
    listener.noMatchFor(r1);
    listener.batchDone();
    listener.endProcessing();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    TestUtils.verifySame(new Link("1", "2", LinkStatus.RETRACTED, LinkKind.SAME, 0.0),
        all.iterator().next());
  }

  @Test
  public void testSingleRecordPerhaps() {
    Record r1 = makeRecord("id", "1");
    Record r2 = makeRecord("id", "2");

    listener.startProcessing();
    listener.batchReady(1);
    listener.matchesPerhaps(r1, r2, 0.7);
    listener.batchDone();
    listener.endProcessing();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    TestUtils.verifySame(new Link("1", "2", LinkStatus.INFERRED, LinkKind.MAYBESAME, 0.7),
        all.iterator().next());
  }

  @Test
  public void testUpgradeFromPerhaps() {
    testSingleRecordPerhaps();
    pause(); // ensure timestamps are different

    Record r1 = makeRecord("id", "1");
    Record r2 = makeRecord("id", "2");

    listener.startProcessing();
    listener.batchReady(1);
    listener.matches(r1, r2, 1.0);
    listener.batchDone();
    listener.endProcessing();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    TestUtils.verifySame(new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME, 1.0),
        all.iterator().next());
  }

  @Test
  public void testOverride() {
    Link l1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(l1);

    Record r1 = makeRecord("id", "1");
    Record r2 = makeRecord("id", "2");

    listener.startProcessing();
    listener.batchReady(1);
    listener.matches(r1, r2, 1.0);
    listener.batchDone();
    listener.endProcessing();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    TestUtils.verifySame(new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME, 1.0),
        all.iterator().next());
  }

  @Test
  public void testOverride2() {
    Link l1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);
    linkdb.assertLink(l1);

    Record r1 = makeRecord("id", "1");
    Record r2 = makeRecord("id", "2");

    listener.startProcessing();
    listener.batchReady(1);
    listener.matches(r1, r2, 1.0);
    listener.batchDone();
    listener.endProcessing();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    TestUtils.verifySame(new Link("1", "2", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0),
        all.iterator().next());
  }

  @Test
  public void testNoMatchFor() {
    Record r1 = makeRecord("id", "1");
    Record r2 = makeRecord("id", "2");
    Record r3 = makeRecord("id", "3");
    Record r4 = makeRecord("id", "4");

    listener.startProcessing();
    listener.batchReady(3);
    listener.matches(r1, r3, 1.0);
    listener.noMatchFor(r2);
    listener.matches(r3, r1, 1.0); // need to repeat this one
    listener.matches(r3, r4, 1.0);
    listener.batchDone();
    listener.endProcessing();

    Link l1 = new Link("1", "3", LinkStatus.INFERRED, LinkKind.SAME, 1.0);
    Link l2 = new Link("3", "4", LinkStatus.INFERRED, LinkKind.SAME, 1.0);

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(2, all.size());
    assertTrue(all.contains(l1));
    assertTrue(all.contains(l2));
  }

  @Test
  public void testEmptyBatch() {
    // when running as a server there are often empty batches
    listener.startProcessing();
    listener.endProcessing();
    // nothing's happened, so there should be no links
    assertTrue(linkdb.getAllLinks().isEmpty());
  }

  @Test
  public void testSingleRecordIdempotent() {
    // we want to verify that seeing the same link twice doesn't cause
    // the timestamp to be updated in the link database
    Record r1 = makeRecord("id", "1");
    Record r2 = makeRecord("id", "2");

    listener.startProcessing();
    listener.batchReady(1);
    listener.matches(r1, r2, 0.95);
    listener.batchDone();
    listener.endProcessing();

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    Link original = all.iterator().next();
    TestUtils.verifySame(new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME, 0.95),
        original);

    listener.startProcessing();
    listener.batchReady(1);
    listener.matches(r1, r2, 0.947);
    listener.batchDone();
    listener.endProcessing();

    all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    Link newlink = all.iterator().next();

    TestUtils.verifySame(new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME, 0.95),
        newlink);

    assertEquals(original.getTimestamp(), newlink.getTimestamp());
  }

  // ===== UTILITIES

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
