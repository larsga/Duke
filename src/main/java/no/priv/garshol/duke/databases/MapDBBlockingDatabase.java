
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

/**
 * A database using blocking to find candidate records, storing the
 * blocks in MapDB on disk.
 */
public class MapDBBlockingDatabase implements Database {
  private Configuration config;
  private Collection<KeyFunction> functions;
  private Map<String, Record> idmap;
  private int window_size;
  private Map<KeyFunction, NavigableMap> func_to_map;
  private DB db;

  public MapDBBlockingDatabase() {
    this.db = DBMaker.
      newFileDB(new File("blocks.db")).
      // asyncWriteEnable().
      // asyncWriteFlushDelay(1000).
      // mmapFileEnable().
      // compressionEnable().
      make();
    this.idmap = db.createHashMap("idmap")
      .valueSerializer(new RecordSerializer())
      .make();

    this.functions = new ArrayList();
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
    return idmap.get(id);
  }

  public Collection<Record> findCandidateMatches(Record record) {
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
    return false;
  }

  public void commit() {
    db.commit();
  }
  
  public void close() {
    db.close();
  }

  public void setKeyFunctions(Collection<KeyFunction> functions) {
    this.functions = functions;
  }

  public Collection<KeyFunction> getKeyFunctions() {
    return functions;
  }
  
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

  public String toString() {
    return "MapDBBlockingDatabase window_size=" + window_size + "\n  " +
      functions;
  }

  private String getId(Record r) {
    for (Property idprop : config.getIdentityProperties()) {
      String v = r.getValue(idprop.getName());
      if (v != null)
        return v;
    }
    return null;
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
      out.write(size);
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
      out.write(free);
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