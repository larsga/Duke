/*
 * Duke@TransformOperation.java
 */
package no.priv.garshol.duke.transforms;

import no.priv.garshol.duke.Record;

/**
 * 
 * @author Olivier Leprince
 * @version $Revision: 1.0 $ 
 */
public interface TransformOperation {
	
	/**
	 * Transform the record
	 * @param r The record to modify
	 * @return The transformed record
	 */
	Record transform(Record r); 

}
