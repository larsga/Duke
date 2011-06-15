
package no.priv.garshol.duke;

import java.util.Collection;

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
  public void batchReady(Collection<Record> batch);
  
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
   * Notification that processing of the current record (the one in
   * the last startRecord(r) call) has ended.
   */
  public void endRecord();
  
}