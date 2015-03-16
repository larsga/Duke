
package no.priv.garshol.duke.databases;

import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NavigableMap;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Configuration;

/**
 * A database using blocking to find candidate records. It's in-memory
 * so capacity is limited, but it's primarily intended as a prototype
 * to test the performance and recall of the blocking approach.
 * @since 1.2
 */
public class InMemoryBlockingDatabase extends AbstractBlockingDatabase {

  public InMemoryBlockingDatabase() {
    super();
    this.idmap = new HashMap();
  }
  
  public void index(Record record) {
    indexById(record);

    // index by key
    for (KeyFunction keyfunc : functions) {
      NavigableMap<String, Collection<Record>> blocks = getBlocks(keyfunc);
      String key = keyfunc.makeKey(record);
      Collection<Record> block = blocks.get(key);
      if (block == null) {
        block = new ArrayList();
        blocks.put(key, block);
      }
      block.add(record);
    }
  }

  public boolean isInMemory() {
    return true;
  }

  public String toString() {
    return "InMemoryBlockingDatabase window_size=" + window_size + "\n  " +
      functions;
  }

  // --- plug in extensions

  protected int addBlock(Collection<Record> candidates,
                         Map.Entry block) {
    Collection<Record> recs = (Collection<Record>) block.getValue();
    candidates.addAll(recs);
    return recs.size();
  }
  
  protected NavigableMap makeMap(KeyFunction keyfunc) {
    return new TreeMap();
  } 
}