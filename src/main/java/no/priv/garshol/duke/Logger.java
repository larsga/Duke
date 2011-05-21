
package no.priv.garshol.duke;

/**
 * <b>Experimental</b> attempt at internal log handling which works
 * naturally on the command-line, doesn't introduce dependencies, and
 * at the same time allows integration with a full logging system.
 * <i>This may go away again if I change my mind.</i>
 */
public interface Logger {

  public void debug(String msg);
  
}