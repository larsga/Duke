
package no.priv.garshol.duke.genetic;

import java.io.IOException;

import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.InMemoryLinkDatabase;
import no.priv.garshol.duke.utils.LinkDatabaseUtils;

/**
 * This oracle looks up the answer in a link file.
 */
public class LinkFileOracle implements Oracle {
  private InMemoryLinkDatabase linkdb;

  public LinkFileOracle(String testfile) throws IOException {
    this.linkdb = new InMemoryLinkDatabase();
    linkdb.setDoInference(true);
    LinkDatabaseUtils.loadTestFile(testfile, linkdb);
  }

  public LinkDatabase getLinkDatabase() {
    return linkdb;
  }

  public LinkKind getLinkKind(String id1, String id2) {
    Link link = linkdb.inferLink(id1, id2);
    if (link == null)
      return LinkKind.DIFFERENT; // we assume missing links are incorrect
    return link.getKind();
  }
}