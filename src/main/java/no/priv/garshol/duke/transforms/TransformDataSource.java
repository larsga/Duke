/*
 * Duke@TransformDataSource.java
 */
package no.priv.garshol.duke.transforms;

import java.util.ArrayList;
import java.util.List;

import no.priv.garshol.duke.DataSource;
import no.priv.garshol.duke.Logger;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;

/**
 * A wrapper arround a DataSource that transforms it via list of operations
 * @author Olivier Leprince
 * @version $Revision: 1.0 $ 
 */
public class TransformDataSource implements DataSource {

	/** The DataSource to transform */
	protected DataSource transformedDataSource;

	/** operations to apply on Records */
	protected List<TransformOperation> operations = new ArrayList<TransformOperation>();
	
	/**
	 * Default constructor
	 * @param source The DataSource to be transformed
	 */
	public TransformDataSource(DataSource source) {
		this.transformedDataSource = source;
	}
	
	@Override
	public RecordIterator getRecords() {
		final RecordIterator srciter = transformedDataSource.getRecords();
		return new RecordIterator() {
			@Override
			public Record next() {
				Record r = srciter.next();
				for (TransformOperation op: operations) {
					r = op.transform(r);
				}
				return r;
			}
			
			@Override
			public boolean hasNext() {
				return srciter.hasNext();
			}
		};
	}

	/**
	 * Just cascade the logger
	 */
	@Override
	public void setLogger(Logger logger) {
		transformedDataSource.setLogger(logger);
	}

	/**
	 * Add an operation
	 * @param oper The TransformOperation
	 */
	public void addOperation(TransformOperation oper) {
		operations.add(oper);
	}

	/**
	 * @return the transformedDataSource
	 */
	public DataSource getTransformedDataSource() {
		return transformedDataSource;
	}

}
