
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
  public void testAddOne() {
    Link link = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME);
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
    Link link = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME);
    linkdb.assertLink(link);
    link = new Link("2", "1", LinkStatus.ASSERTED, LinkKind.SAME);
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
    
    Link link1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME);
    linkdb.assertLink(link1);
    Link link2 = new Link("1", "3", LinkStatus.ASSERTED, LinkKind.SAME);
    linkdb.assertLink(link2);
    Link link3 = new Link("2", "3", LinkStatus.ASSERTED, LinkKind.SAME);

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
    
    Link link1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.DIFFERENT);
    linkdb.assertLink(link1);
    Link link2 = new Link("1", "3", LinkStatus.ASSERTED, LinkKind.DIFFERENT);
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
    
    Link link1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.DIFFERENT);
    linkdb.assertLink(link1);
    Link link2 = new Link("1", "3", LinkStatus.ASSERTED, LinkKind.SAME);
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
  public void testInference2() {
    ((InMemoryLinkDatabase) linkdb).setDoInference(true);
    
    Link link1 = new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME);
    linkdb.assertLink(link1);
    Link link2 = new Link("3", "4", LinkStatus.ASSERTED, LinkKind.SAME);
    linkdb.assertLink(link2);
    Link link3 = new Link("3", "5", LinkStatus.ASSERTED, LinkKind.SAME);
    linkdb.assertLink(link3);
    Link link4 = new Link("4", "5", LinkStatus.ASSERTED, LinkKind.SAME);
    Link link5 = new Link("4", "2", LinkStatus.ASSERTED, LinkKind.SAME);
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
}