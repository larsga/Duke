
package no.priv.garshol.duke;

/**
 * An enum that represents the various possible record linkage
 * strategies that Duke supports.  In deduplication mode these
 * strategies never apply. The set of possible strategies is limited
 * at the moment. Other, more advanced possibilities exist for record
 * linkage, but they are not implemented yet. see the links below for
 * more information.</p>
 *
 * <p>http://code.google.com/p/duke/issues/detail?id=55<br>
 * http://research.microsoft.com/pubs/153478/msr-report-1to1.pdf
 */
public enum LinkageStrategy {

  /**
   * This strategy means matching all records against all records in
   * the other group, and accepting the matches that are above the
   * threshold.
   */
  MATCH_ALL,

  /**
   * In this strategy the first group is considered the master (and is
   * assumed not to have duplicates), whereas the second group is
   * assumed to have duplicates. Thus, matching from the first group
   * to the second we accept all matches above threshold, but matching
   * from the second group to the first we only accept the best match
   * for each record in the second group.
   */
  FIRST_IS_MASTER

}
