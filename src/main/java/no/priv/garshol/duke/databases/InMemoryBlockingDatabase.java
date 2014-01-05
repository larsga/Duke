
package no.priv.garshol.duke.databases;

import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Configuration;

// MapDB could be used for disk backend: http://www.mapdb.org/
// need to make MapDB serializer and deserializer
// DB.createTreeMap returns maker, can maker.valueSerializer()

// Map id -> record
// TreeMap key -> ids

/**
 * A database using blocking to find candidate records. It's in-memory
 * so capacity is limited, but it's primarily intended as a prototype
 * to test the performance and recall of the blocking approach.
 */
public class InMemoryBlockingDatabase implements Database {
  private Configuration config;
  private Collection<KeyFunction> functions;
  private Map<String, Record> idmap;
  private int window_size;
  private Map<KeyFunction, TreeMap> func_to_map;

  public InMemoryBlockingDatabase() {
    this.functions = new ArrayList();
    this.idmap = new HashMap();
    this.func_to_map = new HashMap();
    this.window_size = 5;
  }

  public void setConfiguration(Configuration config) {
    this.config = config;
  }

  public void setOverwrite(boolean overwrite) {
  }

  public void setWindowSize(int window_size) {
    this.window_size = window_size;
  }
  
  public void index(Record record) {
    // index by ID
    for (Property idprop : config.getIdentityProperties())
      for (String id : record.getValues(idprop.getName()))
        idmap.put(id, record);

    // index by key
    for (KeyFunction keyfunc : functions) {
      TreeMap<String, Collection<Record>> blocks = getBlocks(keyfunc);
      String key = keyfunc.makeKey(record);
      Collection<Record> block = blocks.get(key);
      if (block == null) {
        block = new ArrayList();
        blocks.put(key, block);
      }
      block.add(record);
    }
  }

  public Record findRecordById(String id) {
    return idmap.get(id);
  }

  public Collection<Record> findCandidateMatches(Record record) {
    Collection<Record> candidates = new HashSet(); //ArrayList();
    
    for (KeyFunction keyfunc : functions) {
      TreeMap<String, Collection<Record>> blocks = getBlocks(keyfunc);
      String key = keyfunc.makeKey(record);
      // System.out.println("key: '" + key + "'");

      // look up the first block
      Map.Entry<String, Collection<Record>> start = blocks.ceilingEntry(key);
      Map.Entry<String, Collection<Record>> entry = start;
      if (start == null)
        continue;
      
      // add all records from this block
      candidates.addAll(entry.getValue());
      int added = entry.getValue().size();
      // System.out.println("entry '" + entry.getKey() + "' " + added);
      // System.out.println("start: " + start.getValue() + " " + added);
      if (added > window_size * 2)
        continue; // we can't add more candidates from this key function

      // then we navigate downwards from the key
      int added_this_way = added / 2;
      entry = blocks.lowerEntry(entry.getKey());
      while (entry != null && added_this_way < window_size) {
        // System.out.println("entry low: " + entry.getValue() + " " + added_this_way);
        candidates.addAll(entry.getValue());
        added_this_way += entry.getValue().size();
        // System.out.println("entry '" + entry.getKey() + "' " + entry.getValue().size());

        entry = blocks.lowerEntry(entry.getKey());
      }

      // then we navigate upwards from the key
      added_this_way = added / 2;
      entry = blocks.higherEntry(start.getKey());
      while (entry != null && added_this_way < window_size) {
        // System.out.println("entry high: " + entry.getValue() + " " + added_this_way);
        candidates.addAll(entry.getValue());
        added_this_way += entry.getValue().size();
        // System.out.println("entry '" + entry.getKey() + "' " + entry.getValue().size());

        entry = blocks.higherEntry(entry.getKey());
      }
    }

    return candidates;
  }

  public boolean isInMemory() {
    return true;
  }

  public void commit() {
  }
  
  public void close() {
  }

  public void setKeyFunctions(Collection<KeyFunction> functions) {
    this.functions = functions;
  }

  public Collection<KeyFunction> getKeyFunctions() {
    return functions;
  }
  
  public TreeMap<String, Collection<Record>> getBlocks(KeyFunction keyfunc) {
    TreeMap map = func_to_map.get(keyfunc);
    if (map == null) {
      map = new TreeMap();
      func_to_map.put(keyfunc, map);
    }
    return map;
  }

  public String toString() {
    return "InMemoryBlockingDatabase window_size=" + window_size + "\n  " +
      functions;
  }
}