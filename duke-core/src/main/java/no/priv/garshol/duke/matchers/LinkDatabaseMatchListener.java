
package no.priv.garshol.duke.matchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.LinkStatus;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Record;

/**
 * Maintains a LinkDatabase of the recorded matches. Assumes that the
 * same record may be processed several times (for example in
 * different runs), and will keep the database correctly updated.
 *
 * <p><b>WARNING:</b> This class is not thread-safe, so attempting to
 * use it with multiple threads will lead to database corruption.
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

  // the only callbacks we get are matches(), matchesPerhaps(), and
  // noMatchFor(). from these, we need to work out when Duke starts
  // on a new record, and call startRecord_() and endRecord_()
  // accordingly.

  public void matches(Record r1, Record r2, double confidence) {
    if (r1 != current) {
      // we've finished processing the previous record, so make the calls
      if (current != null)
        endRecord_();
      startRecord_(r1);
    }

    String id1 = getIdentity(r1);
    String id2 = getIdentity(r2);
    curlinks.add(new Link(id1, id2, LinkStatus.INFERRED, LinkKind.SAME,
                          confidence));
  }

  public void matchesPerhaps(Record r1, Record r2, double confidence) {
    if (r1 != current) {
      // we've finished processing the previous record, so make the calls
      if (current != null)
        endRecord_();
      startRecord_(r1);
    }

    String id1 = getIdentity(r1);
    String id2 = getIdentity(r2);
    curlinks.add(new Link(id1, id2, LinkStatus.INFERRED, LinkKind.MAYBESAME,
                          confidence));
  }

  public void noMatchFor(Record record) {
    // this is the only call we'll get for this record. it means the
    // previous record has ended, and this one has begun (and will end
    // with the next call, whatever it is)
    if (current != null)
      endRecord_();
    startRecord_(record);
    // next callback will trigger endRecord_()
  }

  // this method is called from the event methods
  public void startRecord_(Record r) {
    current = r;
    curlinks = new ArrayList();
  }

  // this method is called from the event methods
  public void endRecord_() {
    // this is where we actually update the link database. basically,
    // all we need to do is to retract those links which weren't seen
    // this time around, and that can be done via assertLink, since it
    // can override existing links.

    // get all the existing links
    Collection<Link> oldlinks = linkdb.getAllLinksFor(getIdentity(current));

    // build a hashmap so we can look up corresponding old links from
    // new links
    if (oldlinks != null) {
      Map<String, Link> oldmap = new HashMap(oldlinks.size());
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
        else if (sameAs(oldl, newl)) {
          // there's no new information here, so just ignore this
          curlinks.remove(newl);
          oldmap.remove(key); // we don't want to retract the old one
        } else
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
    }

    // okay, now we write it all to the database
    for (Link l : curlinks)
      linkdb.assertLink(l);
  }

  public void batchReady(int size) {
    linkdb.validateConnection();
  }

  public void batchDone() {
    // clearly, this is the end of the previous record
    endRecord_();
    current = null;
    linkdb.commit();
  }

  private String getIdentity(Record r) {
    for (Property p : config.getIdentityProperties()) {
      Collection<String> vs = r.getValues(p.getName());
      if (vs == null)
        continue;
      for (String v : vs)
        return v;
    }
    throw new DukeException("No identity found in record [" +
                            PrintMatchListener.toString(r) + "]");
  }

  private String makeKey(Link l) {
    return l.getID1() + "\t" + l.getID2();
  }

  private boolean sameAs(Link l1, Link l2) {
    // we know the IDs are the same, so we're not going to check those
    return l1.getStatus() == l2.getStatus() &&
      l1.getKind() == l2.getKind();
    // confidence and timestamp are irrelevant
  }
}
