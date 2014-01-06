
package no.priv.garshol.duke.databases;

import java.util.Map;
import java.util.NavigableMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.io.File;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.CompactRecord;

// FIXME:
//  - can we find more options that make performance even better?
//  - keep database in-memory if no file name
//  - must respect overwrite option
//  - what if records already exist? (only if not overwrite)
//  - need to add tests to test suite (req in-memory)
//  - abstract key helper?
//    - include phonetic key functions here
//  - what about dependencies?
//  - code sharing with InMemoryBlockingDatabase
//  - implement block size statistics output

/**
 * A database using blocking to find candidate records, storing the
 * blocks in MapDB on disk.
 * @since 1.2
 */
public class MapDBBlockingDatabase implements Database {
  private Configuration config;
  private Collection<KeyFunction> functions;
  private Map<String, Record> idmap;
  private Map<KeyFunction, NavigableMap> func_to_map;
  private DB db;

  // db configuration properties
  private int window_size;
  private int cache_size;
  private String file;
  
  public MapDBBlockingDatabase() {
    this.functions = new ArrayList();
    this.func_to_map = new HashMap();
    this.window_size = 5;
    this.cache_size = 32768; // MapDB default
  }

  public void setConfiguration(Configuration config) {
    this.config = config;
  }

  public void setOverwrite(boolean overwrite) {
  }

  // ----- CONFIGURATION OPTIONS

  /**
   * Sets the key functions used for blocking.
   */
  public void setKeyFunctions(Collection<KeyFunction> functions) {
    this.functions = functions;
  }
  
  /**
   * Sets the minimum number of records to gather from blocks on each
   * side of the start block. If the start block has more records than
   * twice the window size no neighbouring blocks are searched.
   * Setting window_size = 0 disables searching of neighbouring
   * blocks.
   */
  public void setWindowSize(int window_size) {
    this.window_size = window_size;
  }

  /**
   * Sets the size of the MapDB instance cache. Bigger values give
   * better speed, but require more memory. Default is 32768.
   */
  public void setCacheSize(int cache_size) {
    this.cache_size = cache_size;
  }

  /**
   * Sets the file name (and path) of the MapDB database file. If
   * omitted the database is just kept in-memory.
   */
  public void setFile(String file) {
    this.file = file;
  }
  
  public void index(Record record) {
    if (db == null)
      init();
    
    // index by ID
    for (Property idprop : config.getIdentityProperties())
      for (String id : record.getValues(idprop.getName()))
        idmap.put(id, record);

    // index by key
    for (KeyFunction keyfunc : functions) {
      NavigableMap<String, Block> blocks = getBlocks(keyfunc);
      String key = keyfunc.makeKey(record);
      Block block = blocks.get(key);
      if (block == null) {
        block = new Block();
        blocks.put(key, block);
      }
      block.add(getId(record));
      blocks.put(key, block); // changed the object, so need to write again
    }
  }

  public Record findRecordById(String id) {
    if (db == null)
      init();
    return idmap.get(id);
  }

  public Collection<Record> findCandidateMatches(Record record) {
    if (db == null)
      init();
    Collection<Record> candidates = new HashSet(); //ArrayList();
    
    for (KeyFunction keyfunc : functions) {
      NavigableMap<String, Block> blocks = getBlocks(keyfunc);
      String key = keyfunc.makeKey(record);
      // System.out.println("key: '" + key + "'");

      // look up the first block
      Map.Entry<String, Block> start = blocks.ceilingEntry(key);
      Map.Entry<String, Block> entry = start;
      if (start == null)
        continue;
      
      // add all records from this block
      String[] ids = start.getValue().getIds();
      for (int ix = 0; ix < ids.length && ids[ix] != null; ix++)
        candidates.add(idmap.get(ids[ix]));
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
        ids = entry.getValue().getIds();
        for (int ix = 0; ix < ids.length && ids[ix] != null; ix++)
          candidates.add(idmap.get(ids[ix]));
        added_this_way += entry.getValue().size();
        // System.out.println("entry '" + entry.getKey() + "' " + entry.getValue().size());

        entry = blocks.lowerEntry(entry.getKey());
      }

      // then we navigate upwards from the key
      added_this_way = added / 2;
      entry = blocks.higherEntry(start.getKey());
      while (entry != null && added_this_way < window_size) {
        // System.out.println("entry high: " + entry.getValue() + " " + added_this_way);
        ids = entry.getValue().getIds();
        for (int ix = 0; ix < ids.length && ids[ix] != null; ix++)
          candidates.add(idmap.get(ids[ix]));
        added_this_way += entry.getValue().size();
        // System.out.println("entry '" + entry.getKey() + "' " + entry.getValue().size());

        entry = blocks.higherEntry(entry.getKey());
      }
    }

    return candidates;
  }

  public boolean isInMemory() {
    return file != null;
  }

  public void commit() {
    // having commit here slows things down considerably, probably
    // because it forces writes.
  }
  
  public void close() {
    db.commit();
    db.close();
  }

  public String toString() {
    return "MapDBBlockingDatabase window_size=" + window_size +
      ", cache_size=" + cache_size + "\n  " +
      functions;
  }

  // FIXME: make private?
  public Collection<KeyFunction> getKeyFunctions() {
    return functions;
  }
  
  // FIXME: make private?
  public NavigableMap<String, Block> getBlocks(KeyFunction keyfunc) {
    NavigableMap map = func_to_map.get(keyfunc);
    if (map == null) {
      map = db.createTreeMap(keyfunc.getClass().getName())
        .valueSerializer(new BlockSerializer())
        .make();
      func_to_map.put(keyfunc, map);
    }
    return map;
  }

  private String getId(Record r) {
    for (Property idprop : config.getIdentityProperties()) {
      String v = r.getValue(idprop.getName());
      if (v != null)
        return v;
    }
    return null;
  }

  private void init() {
    db = DBMaker.
      newFileDB(new File(file)).
      asyncWriteEnable().
      asyncWriteFlushDelay(10000).
      mmapFileEnableIfSupported().
      // compressionEnable(). (preliminary testing indicates this is slower)
      cacheSize(cache_size).
      cacheLRUEnable().
      // snapshotEnable().
      // transactionDisable().
      make();
    
    idmap = db.createHashMap("idmap")
      .valueSerializer(new RecordSerializer())
      .make();
  }

  static class Block implements Serializable {
    private int free;
    private String[] ids;

    public Block() {
      this.ids = new String[10];
    }

    public Block(int free, String[] ids) {
      this.free = free;
      this.ids = ids;
    }
    
    public String[] getIds() {
      return ids;
    }

    public void add(String id) {
      if (free >= ids.length) {
        String[] newids = new String[ids.length * 2];
        for (int ix = 0; ix < ids.length; ix++)
          newids[ix] = ids[ix];
        ids = newids;
      }
      ids[free++] = id;
    }

    public int size() {
      return free;
    }
  }

  // ----- SERIALIZERS

  static class BlockSerializer implements Serializable, Serializer<Block> {
    public void serialize(DataOutput out, Block block) throws IOException {
      int size = block.size();
      out.writeInt(size);
      String[] ids = block.getIds();
      for (int ix = 0; ix < size; ix++)
        out.writeUTF(ids[ix]);
    }

    public Block deserialize(DataInput in, int available) throws IOException {
      int free = in.readInt();
      String[] ids = new String[free];
      for (int ix = 0; ix < free; ix++)
        ids[ix] = in.readUTF();
      return new Block(free, ids);
    }

    public int fixedSize() {
      return -1;
    }    
  }

  static class RecordSerializer implements Serializable, Serializer<CompactRecord> {
    public void serialize(DataOutput out, CompactRecord value) throws IOException {
      int free = value.getFree();
      out.writeInt(free);
      String[] s = value.getArray();
      for (int ix = 0; ix < free; ix++)
        out.writeUTF(s[ix]);
    }

    public CompactRecord deserialize(DataInput in, int available) throws IOException {
      int free = in.readInt();
      String[] s = new String[free];
      for (int ix = 0; ix < free; ix++)
        s[ix] = in.readUTF();
      return new CompactRecord(free, s);
    }

    public int fixedSize() {
      return -1;
    }    
  }
}