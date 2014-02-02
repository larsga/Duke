
package no.priv.garshol.duke.utils;

import java.util.Iterator;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;

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