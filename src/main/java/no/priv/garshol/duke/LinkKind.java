
package no.priv.garshol.duke;

/**
 * Represents the meaning of a link between two identities.
 */
public enum LinkKind {
  /**
   * Means we assume the two identities refer to the same real-world object.
   */
  SAME,

  /**
   * Means we think it possible that the two identities refer to the
   * same real-world object.
   */
  MAYBESAME,

  /**
   * Means we assume the two identities refer to different real-world objects.
   */
  DIFFERENT;
}