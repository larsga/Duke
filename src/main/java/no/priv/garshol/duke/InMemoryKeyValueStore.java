
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;

/**
 * A simple key value store that keeps all data in memory.
 */
public class InMemoryKeyValueStore implements KeyValueStore {
  private long nextid;                // next available id
  private Record[] records;           // key into this array is the internal id
  private Map<String, Bucket> tokens; // lookup token -> internal ids
  private Map<String, Long> byid;     // lookup extid -> internal id

  public InMemoryKeyValueStore() {
    this.records = new Record[1000];
    this.tokens = new HashMap();
    this.byid = new HashMap();
  }
  
  public boolean isInMemory() {
    return true;
  }

  public void commit() {
  }
  
  public void close() {
  }

  public long makeNewRecordId() {
    return nextid++;
  }

  public void registerRecord(long id, Record record) {
    // grow array if necessary
    if (id >= records.length) {
      Record[] newbuf = new Record[records.length * 2];
      System.arraycopy(records, 0, newbuf, 0, records.length);
      records = newbuf;
    }

    // register
    records[(int) id] = record;
  }
  
  public void registerId(long id, String extid) {
    byid.put(extid, id);
  }
  
  public void registerToken(long id, String propname, String token) {
    String key = propname + '|' + token;
    Bucket bucket = tokens.get(key);
    if (bucket == null) {
      bucket = new Bucket();
      tokens.put(key, bucket);
    }
    bucket.add(id);
  }

  public Record findRecordById(String extid) {
    Long id = byid.get(extid);
    if (id == null)
      return null;
    return records[id.intValue()];
  }

  public Record findRecordById(long id) {
    return records[(int) id];
  }

  public long[] lookupToken(String propname, String token) {
    return tokens.get(propname + '|' + token).records;
  }

  public String toString() {
    return "InMemoryKeyValueStore";
  }

  // helper class
  static class Bucket {
    private int nextfree;
    private long[] records;

    public Bucket() {
      this.records = new long[10];
    }
    
    public void add(long id) {
      if (nextfree >= records.length) {
        long[] newbuf = new long[records.length * 2];
        System.arraycopy(records, 0, newbuf, 0, records.length);
        records = newbuf;
      }
      records[nextfree++] = id;
    }
  }
}
