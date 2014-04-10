
package no.priv.garshol.duke;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * A link database implementation which keeps everything in memory.
 */
public class InMemoryLinkDatabase implements LinkDatabase {
  // every link is mapped under both ID1 and ID2
  private Map<String,Collection<Link>> links;  
  private boolean infer; // whether to add inferred links explicitly

  public InMemoryLinkDatabase() {
    this.links = new HashMap();
  }

  public void setDoInference(boolean infer) {
    this.infer = infer;
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
    throw new UnsupportedOperationException();
  }
  
  public Collection<Link> getAllLinksFor(String id) {
    Collection<Link> l = links.get(id);
    if (l == null)
      return Collections.EMPTY_SET;
    else
      return l;
  }

  public void assertLink(Link link) {
    // first: check if we already have some version of this link.  if
    // we do, we simply retract it, then carry on as usual.
    boolean found = false;
    Collection<Link> linkset = links.get(link.getID1());
    if (linkset != null)
      for (Link oldlink : linkset)
        if (oldlink.equals(link)) {
          retract(oldlink); // ie: if it involves the same two IDs
          found = true;
          break;
        }

    // do inference, if necessary 
    if (!found && infer) {
      if (link.getKind() == LinkKind.SAME) {
        // inference for SAME links. the idea is that all records in a
        // cluster must have exactly the same links, because they are
        // all equivalent, anyway. therefore, when a node joins the
        // cluster, it must receive copies of all the links for one of
        // the nodes in that cluster. so basically, all nodes in cluster1
        // must receive copies of the links from one node in cluster2, and
        // vice versa.
        copyall(link.getID1(), link.getID2());
        copyall(link.getID2(), link.getID1());
        
      } else if (link.getKind() == LinkKind.DIFFERENT) {
        // inference for DIFFERENT links. the idea is that all records
        // that are the same as one of these two, will be different
        // from one another.
        Collection<String> klass = getClass(link.getID1());
        for (String id : klass)
          addLink2(new Link(id, link.getID2(), link.getStatus(),
                            LinkKind.DIFFERENT, link.getConfidence()));
        klass = getClass(link.getID2());
        for (String id : klass)
          addLink2(new Link(id, link.getID1(), link.getStatus(),
                            LinkKind.DIFFERENT, link.getConfidence()));
      }
      addLink2(link);
    } else
      // add the new link
      addLink(link);
  }

  private void copyall(String id1, String id2) {
    Collection<String> class1 = getClass(id1);
    for (String id : class1) {
      for (Link tocopy : getAllLinksFor(id2)) {
        String other = tocopy.getOtherId(id2);
        if (id.equals(other))
          continue;
        addLink2(new Link(id, other, tocopy.getStatus(), tocopy.getKind(),
                          tocopy.getConfidence()));
      }
    }
  }

  private void addLink(Link link) {
    indexLink(link.getID1(), link);
    indexLink(link.getID2(), link);
  }

  private void addLink2(Link link) {
    // checks for existence first, doesn't add if it already exists
    boolean found = false;
    Collection<Link> linkset = links.get(link.getID1());
    if (linkset != null)
      for (Link oldlink : linkset)
        if (oldlink.equals(link))
          return;

    addLink(link);
  }

  // returns all members of the same equivalence class as @id
  private Collection<String> getClass(String id) {
    Collection<String> klass = new ArrayList();
    klass.add(id);

    for (Link link : getAllLinksFor(id))
      if (link.getKind() == LinkKind.SAME)
        klass.add(link.getOtherId(id));

    return klass;
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
    // if (traverseFrom(id1, id2, new HashSet()))
    //   return new Link(id1, id2, LinkStatus.ASSERTED, LinkKind.SAME, 0.0);
    // else
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
      for (String eqid : traverseAll(id, new HashSet<String>()))
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

  public String toString() {
    return "[InMemoryLinkDatabase size: " + getAllLinks().size() + " infer: " +
      infer + " " + hashCode() + "]";
  }
}
