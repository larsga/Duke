
package no.priv.garshol.duke.datasources;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.utils.DefaultRecordIterator;

/**
 * class for wrapping a simple collection as a datasource for online
 * record linking
 */
public abstract class AbstractMemoryMappedDataSource<T>
  extends ColumnarDataSource {
  /** stores the managed in memory collection */
  private Collection<Record> backend;

  /**
   * convert given entities to Record-objects and store them in the back end
   *  
   * @param entities objects to store
   */
  public AbstractMemoryMappedDataSource(Collection<T> entities) {
    setBackend(new ArrayList<Record>());
    
    for (T entity : entities) {
      getBackend().add(convert(entity));
    }
  }

  /**
   * convert a given entity to a corresponding Record
   */
  protected abstract Record convert(T entity);

  @Override
  protected String getSourceName() {
    return "MEMORY";
  }

  @Override
  public RecordIterator getRecords() {
    return new DefaultRecordIterator(getBackend().iterator());
  }

  public void setBackend(Collection<Record> backend) {
    this.backend = backend;
  }

  public Collection<Record> getBackend() {
    return backend;
  }
}