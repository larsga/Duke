
package no.priv.garshol.duke.matchers;

import java.util.Collection;

import no.priv.garshol.duke.Record;

/**
 * Interface implemented by code which can receive notifications that
 * two records are considered to match.
 */
public interface MatchListener {

  /**
   * Notification that the processor starts to match this record.
   */
  public void startRecord(Record r);

  /**
   * Notification that Duke is about to process a new batch of records.
   */
  public void batchReady(int size);

  /**
   * Notification that Duke has finished processing a batch of records.
   */
  public void batchDone();
  
  /**
   * Notification that the two records match. There will have been a
   * previous startRecord(r1) notification.
   */
  public void matches(Record r1, Record r2, double confidence);

  /**
   * Notification that the two records might match. There will have
   * been a previous startRecord(r1) notification.
   */
  public void matchesPerhaps(Record r1, Record r2, double confidence);

  /**
   * Called in record linkage mode if no link is found for the record.
   */
  public void noMatchFor(Record record);
  
  /**
   * Notification that processing of the current record (the one in
   * the last startRecord(r) call) has ended.
   */
  public void endRecord();

  /**
   * Notification that the processing run is beginning.
   */
  public void startProcessing();

  /**
   * Notification that this processing run is over.
   */
  public void endProcessing();
}