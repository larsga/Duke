
package no.priv.garshol.duke.matchers;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkStatus;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Record;

/**
 * Writes recorded matches to a LinkDatabase.
 */
public class LinkDatabaseMatchListener extends AbstractMatchListener {
  private Configuration config;
  private LinkDatabase linkdb;
  private Record current;
  private Collection<Link> curlinks;

  public LinkDatabaseMatchListener(Configuration config, LinkDatabase linkdb) {
    this.config = config;
    this.linkdb = linkdb;
  }

  public void startRecord(Record r) {
    current = r;
    curlinks = new ArrayList();
  }
  
  public void matches(Record r1, Record r2, double confidence) {
    String id1 = getIdentity(r1);
    String id2 = getIdentity(r2);
    curlinks.add(new Link(id1, id2, LinkStatus.INFERRED, LinkKind.SAME));
  }

  public void matchesPerhaps(Record r1, Record r2, double confidence) {
    String id1 = getIdentity(r1);
    String id2 = getIdentity(r2);
    curlinks.add(new Link(id1, id2, LinkStatus.INFERRED, LinkKind.MAYBESAME));
  }
  
  public void endRecord() {
    // this is where we actually update the link database. basically,
    // all we need to do is to retract those links which weren't seen
    // this time around, and that can be done via assertLink, since it
    // can override existing links.

    // get all the existing links
    Collection<Link> oldlinks = linkdb.getAllLinksFor(getIdentity(current));

    // build a hashmap so that we can look up corresponding old links from
    // new links
    Map<String, Link> oldmap = new HashMap(oldlinks.size() * 2);
    for (Link l : oldlinks)
      oldmap.put(makeKey(l), l);

    // removing all the links we found this time around from the set of
    // old links. any links remaining after this will be stale, and need
    // to be retracted
    for (Link newl : new ArrayList<Link>(curlinks)) {
      String key = makeKey(newl);
      Link oldl = oldmap.get(key);
      if (oldl == null)
        continue;

      if (oldl.overrides(newl))
        // previous information overrides this link, so ignore
        curlinks.remove(newl); 
      else
        // the link is out of date, but will be overwritten, so remove
        oldmap.remove(key);
    }

    // all the inferred links left in oldmap are now old links we
    // didn't find on this pass. there is no longer any evidence
    // supporting them, and so we can retract them.
    for (Link oldl : oldmap.values())
      if (oldl.getStatus() == LinkStatus.INFERRED) {
        oldl.retract(); // changes to retracted, updates timestamp
        curlinks.add(oldl);
      }

    // okay, now we write it all to the database
    for (Link l : curlinks)
      linkdb.assertLink(l);
  }

  public void batchDone() {
    linkdb.commit();
  }
  
  private String getIdentity(Record r) {
    for (Property p : config.getIdentityProperties())
      for (String v : r.getValues(p.getName()))
        return v;
    throw new RuntimeException("No identity found in record [" +
                               PrintMatchListener.toString(r) + "]");
  }

  private String makeKey(Link l) {
    return l.getID1() + "\t" + l.getID2();
  }
  
}