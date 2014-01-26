
package no.priv.garshol.duke.test;

import java.util.Collection;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkStatus;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.InMemoryLinkDatabase;

public class InMemoryLinkDatabaseTest {
  private LinkDatabase linkdb;

  @Before
  public void setUp() {
    this.linkdb = new InMemoryLinkDatabase();
  }

  @Test
  public void emptyTest() {
    assertEquals(linkdb.getAllLinks().size(), 0);
    assertEquals(linkdb.getAllLinksFor("1").size(), 0);
  }

  @Test
  public void testGetAllLinksForEmpty() {
    assertTrue(linkdb.getAllLinksFor("nonexistentid").isEmpty());
  }

  @Test
  public void testAddOne() {
    Link link = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(link);

    Collection<Link> links = linkdb.getAllLinks();
    assertEquals(links.size(), 1);
    assertTrue(links.contains(link));

    links = linkdb.getAllLinksFor("1");
    assertEquals(links.size(), 1);
    assertTrue(links.contains(link));

    links = linkdb.getAllLinksFor("2");
    assertEquals(links.size(), 1);
    assertTrue(links.contains(link));
  }

  @Test
  public void testAddIdempotent() {
    Link link = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(link);
    link = new Link("2", "1", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(link);

    Collection<Link> links = linkdb.getAllLinks();
    assertEquals(links.size(), 1);
    assertTrue(links.contains(link));

    links = linkdb.getAllLinksFor("1");
    assertEquals(links.size(), 1);
    assertTrue(links.contains(link));

    links = linkdb.getAllLinksFor("2");
    assertEquals(links.size(), 1);
    assertTrue(links.contains(link));
  }

  @Test
  public void testInference() {
    ((InMemoryLinkDatabase) linkdb).setDoInference(true);
    
    Link link1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(link1);
    Link link2 = new Link("1", "3", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(link2);
    Link link3 = new Link("2", "3", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);

    Collection<Link> links = linkdb.getAllLinks();
    assertEquals(3, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link2));
    assertTrue(links.contains(link3));

    links = linkdb.getAllLinksFor("1");
    assertEquals(2, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link2));

    links = linkdb.getAllLinksFor("2");
    assertEquals(2, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link3));

    links = linkdb.getAllLinksFor("3");
    assertEquals(2, links.size());
    assertTrue(links.contains(link2));
    assertTrue(links.contains(link3));
  }

  @Test
  public void testInferenceDifferent() {
    ((InMemoryLinkDatabase) linkdb).setDoInference(true);
    
    Link link1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);
    linkdb.assertLink(link1);
    Link link2 = new Link("1", "3", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);
    linkdb.assertLink(link2);

    Collection<Link> links = linkdb.getAllLinks();
    assertEquals(2, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link2));

    links = linkdb.getAllLinksFor("1");
    assertEquals(2, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link2));

    links = linkdb.getAllLinksFor("2");
    assertEquals(1, links.size());
    assertTrue(links.contains(link1));

    links = linkdb.getAllLinksFor("3");
    assertEquals(1, links.size());
    assertTrue(links.contains(link2));
  }

  @Test
  public void testInferenceDifferent2() {
    ((InMemoryLinkDatabase) linkdb).setDoInference(true);
    
    Link link2 = new Link("1", "3", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(link2);
    Link link1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);
    linkdb.assertLink(link1);

    // since 1==3, and 1!=2, it follows that 3!=2, too
    Link link3 = new Link("2", "3", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);
 
    Collection<Link> links = linkdb.getAllLinks();
    assertEquals(3, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link2));
    assertTrue(links.contains(link3));

    links = linkdb.getAllLinksFor("1");
    assertEquals(2, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link2));

    links = linkdb.getAllLinksFor("2");
    assertEquals(2, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link3));

    links = linkdb.getAllLinksFor("3");
    assertEquals(2, links.size());
    assertTrue(links.contains(link2));
    assertTrue(links.contains(link3));
  }

  @Test
  public void testInferenceDifferent2b() {
    ((InMemoryLinkDatabase) linkdb).setDoInference(true);
    
    Link link1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);
    linkdb.assertLink(link1);
    Link link2 = new Link("1", "3", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(link2);

    // since 1==3, and 1!=2, it follows that 3!=2, too
    Link link3 = new Link("2", "3", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);
 
    Collection<Link> links = linkdb.getAllLinks();
    assertEquals(3, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link2));
    assertTrue(links.contains(link3));

    links = linkdb.getAllLinksFor("1");
    assertEquals(2, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link2));

    links = linkdb.getAllLinksFor("2");
    assertEquals(2, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link3));

    links = linkdb.getAllLinksFor("3");
    assertEquals(2, links.size());
    assertTrue(links.contains(link2));
    assertTrue(links.contains(link3));
  }
  
  @Test
  public void testInference2() {
    ((InMemoryLinkDatabase) linkdb).setDoInference(true);
    
    Link link1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(link1);
    Link link2 = new Link("3", "4", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(link2);
    Link link3 = new Link("3", "5", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(link3);
    Link link4 = new Link("4", "5", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    Link link5 = new Link("4", "2", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    linkdb.assertLink(link5);

    Collection<Link> links = linkdb.getAllLinks();
    assertEquals(10, links.size());
    assertTrue(links.contains(link1));
    assertTrue(links.contains(link2));
    assertTrue(links.contains(link3));
    assertTrue(links.contains(link4));
    assertTrue(links.contains(link5));

    links = linkdb.getAllLinksFor("1");
    assertEquals(4, links.size());
    links = linkdb.getAllLinksFor("2");
    assertEquals(4, links.size());
    links = linkdb.getAllLinksFor("3");
    assertEquals(4, links.size());
    links = linkdb.getAllLinksFor("4");
    assertEquals(4, links.size());
    links = linkdb.getAllLinksFor("5");
    assertEquals(4, links.size());
  }

  @Test
  public void testInferenceDifferentBig() {
    ((InMemoryLinkDatabase) linkdb).setDoInference(true);

    // cluster 1
    Link link1 = same("1", "3");
    Link link2 = different("1", "2");
    Link link3 = new Link("2", "3", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);

    // cluster 2
    Link link4 = same("4", "6");
    Link link5 = different("4", "5");
    Link link6 = new Link("5", "6", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);

    // merge the two clusters
    Link link7 = same("3", "4");
    Link link8 = new Link("2", "4", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);
    Link link9 = new Link("2", "6", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);
    Link link10 = new Link("1", "4", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    Link link11 = new Link("1", "6", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    Link link12 = new Link("3", "5", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);
    Link link13 = new Link("1", "5", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 1.0);
    Link link14 = new Link("3", "6", LinkStatus.ASSERTED, LinkKind.SAME, 1.0);
    
    Collection<Link> links = linkdb.getAllLinks();
    verifyContained(links, new Link[] {link1, link2, link3, link4, link5, link6,
                                       link7, link8, link9, link10, link11,
                                       link12, link13, link14});
  }

  // ----- UTILITIES

  private Link same(String id1, String id2) {
    return link(id1, id2, LinkKind.SAME);
  }

  private Link different(String id1, String id2) {
    return link(id1, id2, LinkKind.DIFFERENT);
  }
  
  private Link link(String id1, String id2, LinkKind kind) {
    Link link = new Link(id1, id2, LinkStatus.ASSERTED, kind, 1.0);
    linkdb.assertLink(link);
    return link;
  }

  private void verifyContained(Collection<Link> coll, Link[] links) {
    assertEquals("wrong number of links", links.length, coll.size());
    for (int ix = 0; ix < links.length; ix++)
      assertTrue("correct link not inferred: " + links[ix],
                 coll.contains(links[ix]));
  }
}