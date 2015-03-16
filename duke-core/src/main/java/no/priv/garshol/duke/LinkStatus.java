
package no.priv.garshol.duke;

/**
 * Represents the status of a link between two identities. That is, do
 * we believe it, and why?
 */
public enum LinkStatus {
  /**
   * Means we have outside evidence indicating this is true.
   */
  ASSERTED(2),

  /**
   * Means Duke has worked this out on its own.
   */
  INFERRED(1),

  /**
   * Means Duke used to believe this, but has since changed its mind.
   */
  RETRACTED(0);

  private int id;
  private LinkStatus(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public static LinkStatus getbyid(int id) {
    if (id == 2)
      return ASSERTED;
    else if (id == 1)
      return INFERRED;
    else if (id == 0)
      return RETRACTED;
    throw new DukeException("No status with id " + id);
  }
}