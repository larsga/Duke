
package no.priv.garshol.duke;

/**
 * Used to signal that something has gone wrong during Duke
 * processing.
 */
public class DukeException extends RuntimeException {

  public DukeException(String msg, Exception e) {
    super(msg, e);
  }

  public DukeException(Exception e) {
    super(e);
  }
  
}