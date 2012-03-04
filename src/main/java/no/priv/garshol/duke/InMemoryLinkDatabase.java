
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
    throw new RuntimeException("not implemented yet"); 
  }
  
  public List<Link> getChangesSince(long since) {
    throw new RuntimeException("not implemented yet"); 
  }
  
  public Collection<Link> getAllLinksFor(String id) {
    return links.get(id);
  }

  public void assertLink(Link link) {
    indexLink(link.getID1(), link);
    indexLink(link.getID2(), link);
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
  
  public void commit() {
    // we have nowhere to commit to
  }
}