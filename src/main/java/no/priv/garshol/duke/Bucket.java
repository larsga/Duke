
package no.priv.garshol.duke;

public class Bucket {
  public int nextfree;
  // if the bucket has gone over size, this is 'null'
  public long[] records;
  // if buckets go over this size, discard contents
  private static final int MAX_BUCKET_SIZE = 1000;

  public Bucket() {
    this.records = new long[10];
  }
    
  public void add(long id) {
    if (records == null)
      return; // bucket went over size, now discarding all records
    
    if (nextfree >= records.length) {
      if (nextfree >= MAX_BUCKET_SIZE) {
        // this bucket is now oversized
        records = null;
        return;
      }
        
      long[] newbuf = new long[Math.min(records.length * 2, MAX_BUCKET_SIZE)];
      System.arraycopy(records, 0, newbuf, 0, records.length);
      records = newbuf;
    }
    records[nextfree++] = id;
  }
}