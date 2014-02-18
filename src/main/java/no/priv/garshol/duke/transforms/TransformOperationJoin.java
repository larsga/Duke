/*
 * Duke@TransformOperationJoin.java
 */
package no.priv.garshol.duke.transforms;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.utils.StringUtils;

/**
 * A specific TransformOperation that add to the record an additional property that is the result of a join operation between properties.
 */
public class TransformOperationJoin implements TransformOperation {

	protected String resultingProperty;

	protected String[] properties;

	protected String joiner = " ";
	
	/**
	 * @see no.priv.garshol.duke.transforms.TransformOperation#transform(no.priv.garshol.duke.Record)
	 */
	@Override
	public Record transform(Record record) {
		
		StringBuilder tmp = new StringBuilder();
		boolean first = true;
		for (int i = 0; i < properties.length; i++) {
			String v = record.getValue(properties[i]);
			if (v!=null && !v.equals("")) {
				if (!first) {
					tmp.append(joiner);
				}
				first = false;
				tmp.append(v);
			}
		}
		return new TransformedRecord(record, resultingProperty, tmp.toString());
	}

	//--------------------------------- configuration --
	
	/**
	 * @param resultingProperty the resultingProperty to set
	 */
	public void setResultingProperty(String resultingProperty) {
		this.resultingProperty = resultingProperty;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(String props) {
		this.properties =  StringUtils.split(props);
	}

	/**
	 * @param joiner the joiner to set
	 */
	public void setJoiner(String joiner) {
		this.joiner = joiner;
	}

}
