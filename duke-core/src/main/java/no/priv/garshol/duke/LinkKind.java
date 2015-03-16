  
package no.priv.garshol.duke;

/**
 * Represents the meaning of a link between two identities.
 */
public enum LinkKind {
  /**
   * Means we assume the two identities refer to the same real-world object.
   */
  SAME(1),

  /**
   * Means we think it possible that the two identities refer to the
   * same real-world object.
   */
  MAYBESAME(2),

  /**
   * Means we assume the two identities refer to different real-world objects.
   */
  DIFFERENT(3);

  private int id;
  private LinkKind(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public static LinkKind getbyid(int id) {
    if (id == 1)
      return SAME;
    else if (id == 2)
      return MAYBESAME;
    else if (id == 3)
      return DIFFERENT;
    throw new DukeException("No kind with id " + id);
  }
}