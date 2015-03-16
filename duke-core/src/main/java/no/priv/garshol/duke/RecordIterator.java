
package no.priv.garshol.duke;

import java.io.Closeable;
import java.util.Iterator;

/**
 * Special Iterator class for Record collections, in order to add some
 * extra methods for resource management.
 */
public abstract class RecordIterator
  implements Iterator<Record>, Closeable {

  /**
   * Releases any resources held by this iterator, and cleans up any
   * temporary storage.
   */
  public void close() {
  }

  /**
   * Informs the iterator that the latest batch of records retrieved
   * from the iterator has been processed. This may in some cases
   * allow iterators to free resources, but iterators are not required
   * to perform any action in response to this call.
   */
  public void batchProcessed() {
  }
  
  public void remove() {
    throw new UnsupportedOperationException();
  }
  
}