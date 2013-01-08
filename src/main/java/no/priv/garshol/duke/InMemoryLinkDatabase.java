
package no.priv.garshol.duke;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A link database implementation which keeps everything in memory.
 */
public class InMemoryLinkDatabase implements LinkDatabase {
  // every link is mapped under both ID1 and ID2
  private Map<String,Collection<Link>> links;

  public InMemoryLinkDatabase() {
    this.links = new HashMap();
  }
  
  public List<Link> getAllLinks() {
    // contains the links we've already covered, form: ID1,ID2
    Set<String> seen = new HashSet();
    
    List<Link> all = new ArrayList();
    for (Collection<Link> linkcoll : links.values())
      for (Link link : linkcoll) {
        String id = link.getID1() + ',' + link.getID2();
        if (seen.contains(id))
          continue;

        all.add(link);
        seen.add(id);
      }

    return all;
  }
  
  public List<Link> getChangesSince(long since) {
    throw new RuntimeException("not implemented yet"); 
  }
  
  public Collection<Link> getAllLinksFor(String id) {
    return links.get(id);
  }

  public void assertLink(Link link) {
    // first: check if we already have some version of this link.  if
    // we do, we simply retract it, then carry on as usual.
    Collection<Link> linkset = links.get(link.getID1());
    if (linkset != null) {
      for (Link oldlink : linkset)
        if (oldlink.equals(link)) {
          retract(oldlink);
          break;
        }
    }

    // add the new link
    indexLink(link.getID1(), link);
    indexLink(link.getID2(), link);
  }

  private void retract(Link link) {
    // it's indexed under both IDs, so we need to remove it from both
    // places
    links.get(link.getID1()).remove(link);
    links.get(link.getID2()).remove(link);
  }

  private void indexLink(String id, Link link) {
    Collection<Link> ourlinks = links.get(id);
    if (ourlinks == null) {
      ourlinks = new ArrayList();
      links.put(id, ourlinks);
    }
    ourlinks.add(link);
  }

  public Link inferLink(String id1, String id2) {
    // FIXME: it's possible that we find inconsistencies here. for now we
    // ignore that. if we've seen a link between these two IDs then that
    // means we're not going to ask the user about it.
    Collection<Link> ourlinks = links.get(id1);
    if (ourlinks != null) {
      for (Link link : ourlinks)
        if (link.getID1().equals(id2) || link.getID2().equals(id2))
          return link;
      // if we get here it means we couldn't find a direct link. move on
      // to see if we can find an indirect one.
    }
    
    // can we prove that these belong to the same equivalence class?
    // basically, need to traverse graph outwards from ID1 to see if
    // we ever get to ID2.
    if (traverseFrom(id1, id2, new HashSet()))
      return new Link(id1, id2, LinkStatus.ASSERTED, LinkKind.SAME);
    else
      return null;
  }

  // returns true if we succeed in finding a path from ID1 to ID2
  private boolean traverseFrom(String id, String goalid, Set<String> seen) {
    seen.add(id);
    if (links.get(id) == null)
      return false;
    
    for (Link link : links.get(id)) {
      // check that this is a SAME link
      if (link.getKind() != LinkKind.SAME)
        continue;
      
      // find the ID that is not 'id' (find the ID at the other end)
      String otherid = link.getID1();
      if (otherid.equals(id))
        otherid = link.getID2();

      // if we haven't seen it, and it's not goalid, keep traversing
      if (otherid.equals(goalid))
        return true;
      else if (!seen.contains(otherid)) {
        if (traverseFrom(otherid, goalid, seen))
          return true; // found it!

        // else: keep trying
      }
    }
    return false;
  }

  public void checkConsistency() {
    for (String id : links.keySet()) {
      // find all IDs which we are *not* equal to
      Set<String> diff = new HashSet();
      for (Link link : links.get(id))
        if (link.getKind() == LinkKind.DIFFERENT)
          diff.add(link.getOtherId(id));

      // then, find all IDs which we, implicity or explicitly, are equal to
      for (String eqid : traverseAll(id, new HashSet()))
        if (diff.contains(eqid))
          System.out.println("Inconsistency: " + id + " <-> " + eqid);
    }
  }

  public Set<String> traverseAll(String id, Set<String> seen) {
    seen.add(id);
    for (Link link : links.get(id)) {
      String other = link.getOtherId(id);
      if (link.getKind() == LinkKind.SAME && !seen.contains(other))
        traverseAll(other, seen);
    }

    return seen;
  }

  public void validateConnection() {
    // nothing to do
  }
  
  public void commit() {
    // we have nowhere to commit to
  }

  public void clear() {
    links.clear();
  }

  public void close() {
    // nothing to do
  }
}