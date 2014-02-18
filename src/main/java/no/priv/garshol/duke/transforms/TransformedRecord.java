/*
 * Duke@TransformedRecord.java
 * Copyright ALSTOM ITC - EIS CC
 * All rights reserved.
 */
package no.priv.garshol.duke.transforms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import no.priv.garshol.duke.Record;

/**
 * A transformed record that add a virtual property on the record built by using the transform method
 * @author Olivier Leprince
 * @version $Revision: 1.0 $
 */
public class TransformedRecord implements Record {
	
	/** The name of the virtual property */
	protected String resultingProperty;
	/** The value of the virtual property */
	protected String resultingValue;

	/** The record that is extended */
	protected Record record;

	/** The backed up list of properties */
	protected Collection<String> props;
	
	public TransformedRecord(Record r, String resultingColumn, String resultingValue) {
		this.record = r;
		this.resultingProperty = resultingColumn;
		this.resultingValue = resultingValue;

		this.props = new ArrayList<String>(r.getProperties());
		this.props.add(resultingColumn);
	}

	@Override
	public Collection<String> getProperties() {
		return this.props;
	}

	@Override
	public Collection<String> getValues(String prop) {
		if (prop.equals(resultingProperty)) {
			return Collections.singleton(resultingValue);
		} else {
			return record.getValues(prop);
		}
	}

	@Override
	public String getValue(String prop) {
		if (prop.equals(resultingProperty)) {
			return resultingValue;
		} else {
			return record.getValue(prop);
		}
	}

	@Override
	public void merge(Record other) {
		record.merge(other);
	}

}
