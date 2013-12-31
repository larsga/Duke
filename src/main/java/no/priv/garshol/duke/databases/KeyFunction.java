
package no.priv.garshol.duke.databases;

import no.priv.garshol.duke.Record;

/**
 * A key function produces a blocking key from a record.
 */
public interface KeyFunction {

  public String makeKey(Record record);

}
