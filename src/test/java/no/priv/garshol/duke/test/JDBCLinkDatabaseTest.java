
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
import static junit.framework.Assert.fail;

import no.priv.garshol.duke.JDBCLinkDatabase;
import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkStatus;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.matchers.LinkDatabaseMatchListener;

// Note that a fair amount of testing takes place already in
// LinkDatabaseMatchListenerTest. Here we only test what isn't tested
// there.

public class JDBCLinkDatabaseTest {
  private JDBCLinkDatabase linkdb;
  
  @Before
  public void setup() {
    linkdb = new JDBCLinkDatabase("org.h2.Driver", "jdbc:h2:mem:", "h2",
                                  new Properties());
    linkdb.init();
  }

  @After
  public void cleanup() {
    linkdb.clear();
    linkdb.close();
  }
  
  @Test
  public void testBadDatabase() {
    try {
      new JDBCLinkDatabase("org.h2.Driver", "jdbc:h2:mem:", "unknowndb",
                           new Properties());
    } catch (DukeConfigException e) {
      // this is what we expect
    }
  }

  @Test
  public void testGetSinceForeverEmpty() {
    assertTrue(linkdb.getChangesSince(0).isEmpty());
  }
  
  @Test
  public void testGetSinceForever() {
    Link l1 = new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l1);
    
    Link l2 = new Link("1", "3", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l2);

    List<Link> links = linkdb.getChangesSince(0);
    assertEquals(2, links.size());

    // we don't know the order, so must check
    Link ll1;
    Link ll2;
    if (links.get(0).equals(l2)) {
      ll1 = links.get(1);
      ll2 = links.get(0);
    } else {
      ll1 = links.get(0);
      ll2 = links.get(1);
    }
    assertEquals(l1, ll1);
    assertEquals(l2, ll2);
  }

  @Test
  public void testGetSinceOnlyHalf() {
    Link l1 = new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l1);

    pause();
    long thetime = System.currentTimeMillis();
    pause();
    
    Link l2 = new Link("1", "3", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l2);

    List<Link> links = linkdb.getChangesSince(thetime);
    assertEquals(1, links.size());
    assertEquals(l2, links.get(0));
  }

  @Test
  public void testGetBeforeOnlyHalf() {
    Link l1 = new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l1);

    pause();
    long thetime = System.currentTimeMillis();
    pause();
    
    Link l2 = new Link("1", "3", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l2);

    List<Link> links = linkdb.getChangesSince(0, thetime);
    assertEquals(1, links.size());
    assertEquals(l1, links.get(0));
  }

  @Test
  public void testGetBeforeAndSince() {
    long since = System.currentTimeMillis();
    pause();
    
    Link l1 = new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l1);

    pause();
    long before = System.currentTimeMillis();
    pause();
    
    Link l2 = new Link("1", "3", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l2);

    List<Link> links = linkdb.getChangesSince(since, before);
    assertEquals(1, links.size());
    assertEquals(l1, links.get(0));
  }
  
  @Test
  public void testGetPageOnlyOne() {
    Link l1 = new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l1);
    pause();
    
    Link l2 = new Link("1", "3", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l2);
    pause();
    
    Link l3 = new Link("1", "4", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l3);
    pause();
    
    Link l4 = new Link("1", "5", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l4);
    pause();

    pause();
    long thetime = System.currentTimeMillis();
    
    List<Link> links = linkdb.getChangesSince(0, thetime + 200, 1);
    assertEquals(1, links.size());
    LinkDatabaseMatchListenerTest.verifySame(l4, links.get(0));
  }
  
  @Test
  public void testOverride() {
    Link l1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME);
    linkdb.assertLink(l1);
    Link l2 = new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l2);

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    LinkDatabaseMatchListenerTest.verifySame(new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME),
               all.iterator().next());
  }  
  
  @Test
  public void testOverride2() {
    Link l1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.DIFFERENT);
    linkdb.assertLink(l1);
    Link l2 = new Link("1", "2", LinkStatus.INFERRED, LinkKind.SAME);
    linkdb.assertLink(l2);

    Collection<Link> all = linkdb.getAllLinks();
    assertEquals(1, all.size());
    LinkDatabaseMatchListenerTest.verifySame(new Link("1", "2", LinkStatus.ASSERTED, LinkKind.DIFFERENT),
               all.iterator().next());
  }
  
  private void pause() {
    try {
      Thread.sleep(10); // ensure that timestamps are different
    } catch (InterruptedException e) {      
    }
  }
}
