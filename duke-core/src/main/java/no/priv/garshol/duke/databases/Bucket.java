
package no.priv.garshol.duke.databases;

import java.util.Arrays;

public class Bucket implements Comparable<Bucket> {
  // the index of the next free cell in the array (== size())
  public int nextfree;
  // if the bucket has gone over size, this is 'null'
  public long[] records;
  // true iff new records have been added to the bucket since last sorting
  private boolean dirty;
  // if buckets go over this size, discard contents
  private static final int MAX_BUCKET_SIZE = 1000000;

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
        dirty = false;
        return;
      }
        
      long[] newbuf = new long[Math.min(records.length * 2, MAX_BUCKET_SIZE)];
      System.arraycopy(records, 0, newbuf, 0, records.length);
      records = newbuf;
    }
    records[nextfree++] = id;
    dirty = true;
  }

  public int compareTo(Bucket other) {
    return nextfree - other.nextfree;
  }

  public void sort() {
    if (!dirty)
      return;

    Arrays.sort(records, 0, nextfree);
    dirty = false;
  }

  public double getScore() {
    //return 1.0 / (double) nextfree;
    if (nextfree == 0)
      return 1.0;
    else
      return 1.0 / Math.log((double) (nextfree + 1));
  }

  public boolean contains(long record) {
    return Arrays.binarySearch(records, 0, nextfree, record) >= 0;
  }
}