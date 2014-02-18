/*
 * Duke@TransformOperation.java
 */
package no.priv.garshol.duke.transforms;

import no.priv.garshol.duke.Record;

/**
 * Operations to apply on Record to transform it
 */
public interface TransformOperation {
	
	/**
	 * Transform the record
	 * @param r The record to modify
	 * @return The transformed record
	 */
	Record transform(Record r); 

}
