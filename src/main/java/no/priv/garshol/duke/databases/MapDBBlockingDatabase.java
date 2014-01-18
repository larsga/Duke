
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
//  - get rid of the <path> element

//  - how on earth can cache make it slower? WAITING
//  - must respect overwrite option

//  - what if records already exist? (only if not overwrite and not in-mem)
//    in that case we'll overwrite id -> record association
//    but old keys will still be there
//    - add test for this to test suite?

/**
 * A database using blocking to find candidate records, storing the
 * blocks in MapDB on disk.
 * @since 1.2
 */
public class MapDBBlockingDatabase extends AbstractBlockingDatabase {
  private DB db;

  // db configuration properties
  private int cache_size;
  private String file;
  private boolean async;
  private boolean mmap;
  private boolean compression;
  private boolean cache;
  private boolean snapshot;
  private boolean notxn;
  
  public MapDBBlockingDatabase() {
    super();
    this.cache_size = 32768; // MapDB default

    // experiments show optimal performance with these two on, and
    // the others off. therefore setting that as default
    this.async = true;
    this.mmap = true;
  }

  // ----- CONFIGURATION OPTIONS

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

  // these configuration options are experimental
  public void setAsync(boolean async) { this.async = async; }
  public void setMmap(boolean mmap) { this.mmap = mmap; }
  public void setCompression(boolean compression) { this.compression = compression; }
  public void setCache(boolean cache) { this.cache = cache; }
  public void setSnapshot(boolean snapshot) { this.snapshot = snapshot; }
  public void setNotxn(boolean notxn) { this.notxn = notxn; }
  
  public void index(Record record) {
    if (db == null)
      init();

    indexById(record);

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
    return super.findCandidateMatches(record);
  }
  
  public boolean isInMemory() {
    return file == null;
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
      ", cache_size=" + cache_size + ", in-memory=" + isInMemory() + "\n  " +
      "async=" + async + ", mmap=" + mmap + ", compress=" + compression +
      ", cache=" + cache + "\n  snapshot=" + snapshot + ", notxn=" + notxn +
      "\n  " +
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

  private void init() {
    DBMaker maker;
    if (file == null)
      maker = DBMaker.newMemoryDB();
    else {
      maker = DBMaker.newFileDB(new File(file));
      if (async) {
        maker = maker.asyncWriteEnable();
        maker = maker.asyncWriteFlushDelay(10000);
      }
      if (mmap)
        maker = maker.mmapFileEnableIfSupported();
      if (compression) 
        maker = maker.compressionEnable();
      if (cache) {
        maker = maker.cacheSize(cache_size);
        maker = maker.cacheLRUEnable();
      }
      if (snapshot)
        maker = maker.snapshotEnable();
      if (notxn)
        maker = maker.transactionDisable();
    }
    
    db = maker.make();
    
    idmap = db.createHashMap("idmap")
      .valueSerializer(new RecordSerializer())
      .make();
  }

  // --- PLUG IN EXTENSIONS

  protected int addBlock(Collection<Record> candidates,
                         Map.Entry block) {
    String[] ids = ((Block) block.getValue()).getIds();
    int ix = 0;
    for (; ix < ids.length && ids[ix] != null; ix++)
      candidates.add(idmap.get(ids[ix]));
    return ix;
  }
  
  protected NavigableMap makeMap(KeyFunction keyfunc) {
    if (db == null)
      init();
    return db.createTreeMap(keyfunc.getClass().getName())
      .valueSerializer(new BlockSerializer())
      .make();
  } 
  
  // --- BLOCK CONTAINER
  
  public static class Block implements Serializable {
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