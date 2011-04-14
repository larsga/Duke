
package no.priv.garshol.duke;

import java.util.Iterator;

public class DefaultRecordIterator extends RecordIterator {
  private Iterator<Record> it;
  
  public DefaultRecordIterator(Iterator<Record> it) {
    this.it = it;
  }

  public boolean hasNext() {
    return it.hasNext();
  }

  public Record next() {
    return it.next();
  }
  
}