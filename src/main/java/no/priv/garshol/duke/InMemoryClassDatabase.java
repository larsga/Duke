
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * An equivalence class database which maintains the entire structure
 * in memory.
 */
public class InMemoryClassDatabase implements EquivalenceClassDatabase {
  /**
   * Index from record ID to class ID.
   */
  private Map<String, Integer> recordix;
  /**
   * The actual collection of classes.
   */
  private Map<Integer, Collection<String>> classix;
  private int nextid;

  /**
   * Instantiates an empty class database.
   */
  public InMemoryClassDatabase() {
    this.recordix = new HashMap();
    this.classix = new HashMap();
  }

  public int getClassCount() {
    return classix.size();
  }

  public Iterator<Collection<String>> getClasses() {
    return classix.values().iterator();
  }
  
  public Collection<String> getClass(String id) {
    Integer cid = recordix.get(id);
    if (cid == null)
      return Collections.EMPTY_SET;
    return classix.get(cid);
  }
  
  public void addLink(String id1, String id2) {
    Integer cid1 = recordix.get(id1);
    Integer cid2 = recordix.get(id2);

    if (cid1 == null && cid2 == null) {
      // need to make a new class
      Integer cid = Integer.valueOf(nextid++);
      Collection<String> klass = new ArrayList();
      klass.add(id1);
      klass.add(id2);
      classix.put(cid, klass);
      recordix.put(id1, cid);
      recordix.put(id2, cid);
    } else if (cid1 == null || cid2 == null) {
      // only one has a class, so add the other to the same class, and
      // we're done
      Integer cid = cid1;
      String id = id2;
      if (cid == null) {
        cid = cid2;
        id = id1;
      }
      Collection<String> klass = classix.get(cid);
      klass.add(id);
      recordix.put(id, cid);
    } else {
      // both records already have a class
      if (cid1.equals(cid2))
        return; // it's the same class, so nothing new learned
      // okay, we need to merge the classes
      merge(cid1, cid2);
    }
  }
  
  public void commit() {
    // nothing to commit
  }

  /**
   * Merges the two classes into a single class. The smaller class is
   * removed, while the largest class is kept.
   */
  private void merge(Integer cid1, Integer cid2) {
    Collection<String> klass1 = classix.get(cid1);
    Collection<String> klass2 = classix.get(cid2);

    // if klass1 is the smaller, swap the two
    if (klass1.size() < klass2.size()) {
      Collection<String> tmp = klass2;
      klass2 = klass1;
      klass1 = tmp;

      Integer itmp = cid2;
      cid2 = cid1;
      cid1 = itmp;
    }

    // now perform the actual merge
    for (String id : klass2) {
      klass1.add(id);
      recordix.put(id, cid1);
    }

    // delete the smaller class, and we're done
    classix.remove(cid2);
  }
  
}