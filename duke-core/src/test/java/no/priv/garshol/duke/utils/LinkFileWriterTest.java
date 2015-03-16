
package no.priv.garshol.duke.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static no.priv.garshol.duke.utils.TestUtils.verifySame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LinkFileWriterTest {
  private File file;
  private FileWriter writer;
  private LinkFileWriter out;
  private LinkDatabase db;
  
  @Rule
  public TemporaryFolder tmpdir = new TemporaryFolder();

  @Before
  public void setup() throws IOException {
    file = tmpdir.newFile("links.txt");
    writer = new FileWriter(file);
    out = new LinkFileWriter(writer);
  }

  @Test
  public void testEmpty() throws IOException {
    writer.close();

    load();

    assertEquals("shouldn't contain links", 0, db.getAllLinks().size());
  }

  @Test
  public void testSingleLink() throws IOException {
    out.write("1", "2", true, 0.95);
    writer.close();

    load();

    assertEquals(1, db.getAllLinks().size());
    Link link = db.getAllLinks().iterator().next();
    verifySame(new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME, 0.95),
               link);
  }

  @Test
  public void testThreeLink() throws IOException {
    out.write("1", "2", true, 0.95);
    out.write("1", "3", false, 0.2);
    out.write("3", "4", true, 0.8);
    writer.close();

    load();

    assertEquals(3, db.getAllLinks().size());
    Collection<Link> links = db.getAllLinks();

    Link link = find(links, "1", "2");
    verifySame(new Link("1", "2", LinkStatus.ASSERTED, LinkKind.SAME, 0.95),
               link);
    
    link = find(links, "1", "3");
    verifySame(new Link("1", "3", LinkStatus.ASSERTED, LinkKind.DIFFERENT, 0.2),
               link);

    link = find(links, "3", "4");
    verifySame(new Link("3", "4", LinkStatus.ASSERTED, LinkKind.SAME, 0.8),
               link);
  }

  private Link find(Collection<Link> links, String id1, String id2) {
    for (Link link : links)
      if (link.getID1().equals(id1) && link.getID2().equals(id2))
        return link;
    fail("Couldn't find link " + id1 + " " + id2);
    return null;
  }
  
  private void load() throws IOException {
    db = LinkDatabaseUtils.loadTestFile(file.getAbsolutePath());
  }
}