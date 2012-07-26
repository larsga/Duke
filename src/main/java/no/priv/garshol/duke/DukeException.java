
package no.priv.garshol.duke;

/**
 * Used to signal that something has gone wrong during Duke
 * processing.
 */
public class DukeException extends RuntimeException {

  public DukeException(String msg) {
    super(msg);
  }
  
  public DukeException(String msg, Throwable e) {
    super(msg, e);
  }

  public DukeException(Throwable e) {
    super(e);
  }
  
}