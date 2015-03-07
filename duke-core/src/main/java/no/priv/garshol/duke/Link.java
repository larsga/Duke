
package no.priv.garshol.duke;

import java.util.Collection;

/**
 * Immutable representation of a link between two identities.
 */
public class Link {
  private String id1;
  private String id2;
  private LinkStatus status;
  private LinkKind kind;
  private long timestamp;
  private double confidence;

  public Link(String id1, String id2, LinkStatus status, LinkKind kind,
              double confidence) {
    if (id1.compareTo(id2) < 0) {
      this.id1 = id1;
      this.id2 = id2;
    } else {
      this.id1 = id2;
      this.id2 = id1;
    }
    this.status = status;
    this.kind = kind;
    this.confidence = confidence;
    this.timestamp = System.currentTimeMillis();
  }

  public Link(String id1, String id2, LinkStatus status, LinkKind kind,
              long timestamp, double confidence) {
    this(id1, id2, status, kind, confidence);
    this.timestamp = timestamp;
  }
  
  public String getID1() { // ID of record, lexiographically lowest
    return id1;
  }
  
  public String getID2() { // ID of record
    return id2;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public LinkStatus getStatus() {
    return status;
  }

  public LinkKind getKind() {
    return kind;
  }

  /**
   * Returns the confidence we have in this link (0.0-1.0). This is
   * the same as the probability reported by Duke.
   * @since 1.2
   */
  public double getConfidence() {
    return confidence;
  }

  /**
   * Get the ID (out of ID1 and ID2) which is not equal to the given ID.
   */
  public String getOtherId(String id) {
    if (id.equals(id1))
      return id2;
    else
      return id1;
  }

  /**
   * Changes the link status to retracted, and updates the timestamp.
   * Does <em>not</em> write to the database. Sets the confidence to
   * 0.0.
   */
  public void retract() {
    status = LinkStatus.RETRACTED;
    confidence = 0.0;
    timestamp = System.currentTimeMillis();
  }

  /**
   * Returns true if the information in this link should take
   * precedence over the information in the other link.
   */
  public boolean overrides(Link other) {
    if (other.getStatus() == LinkStatus.ASSERTED &&
        status != LinkStatus.ASSERTED)
      return false;
    else if (status == LinkStatus.ASSERTED &&
             other.getStatus() != LinkStatus.ASSERTED)
      return true;

    // the two links are from equivalent sources of information, so we
    // believe the most recent

    return timestamp > other.getTimestamp();
  }

  public boolean equals(Object other) {
    if (!(other instanceof Link))
      return false;

    Link olink = (Link) other;
    return (olink.getID1().equals(id1) && olink.getID2().equals(id2));
  }

  public int hashCode() {
    return id1.hashCode() + id2.hashCode();
  }

  public String toString() {
    return "[Link " + id1 + "=" + id2 + " " + kind + " " + status + "]";
  }
}