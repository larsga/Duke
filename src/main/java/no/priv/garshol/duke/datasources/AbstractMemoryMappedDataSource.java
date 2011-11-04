package no.priv.garshol.duke.datasources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;


/**
 * class for wrapping a simple collection as a datasource for online record linking
 */
public abstract class AbstractMemoryMappedDataSource<T> extends ColumnarDataSource {
	/** stores the managed in memory collection */
	private Collection<Record> backend;

	/**
	 * wrap the collection's iterator interface to the RecordIterator
	 */
	public class MemoryRecordIterator extends RecordIterator {
		Collection<Record> backend;
		Iterator<Record> it;

		public MemoryRecordIterator(Collection<Record> backend) {
			this.backend = backend;
			this.it = backend.iterator();
		}

		@Override
		public boolean hasNext() {
			return this.it.hasNext();
		}

		@Override
		public Record next() {
			return this.it.next();
		}

	}

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
	 * 
	 * @param entity
	 * @return
	 */
	protected abstract Record convert(T entity);

	@Override
	protected String getSourceName() {
		return "MEMORY";
	}

	@Override
	public RecordIterator getRecords() {
		return new MemoryRecordIterator(getBackend());
	}

	public void setBackend(Collection<Record> backend) {
		this.backend = backend;
	}

	public Collection<Record> getBackend() {
		return backend;
	}
}