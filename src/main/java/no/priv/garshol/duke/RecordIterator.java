
package no.priv.garshol.duke;

import java.util.Iterator;

public abstract class RecordIterator implements Iterator<Record> {

  /**
   * Releases any resources held by this iterator, and cleans up any
   * temporary storage.
   */
  public void close() {
  }
    
  public void remove() {
    throw new UnsupportedOperationException();
  }
  
}