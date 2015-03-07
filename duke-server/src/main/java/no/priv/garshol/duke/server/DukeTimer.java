
package no.priv.garshol.duke.server;

import java.util.Properties;

public interface DukeTimer {

  /**
   * Initializes the timer, giving it access to configuration settings.
   */
  public void init(Properties props);
  
  /**
   * Starts a background thread which calls the controller every
   * check_interval seconds. Returns immediately, leaving the
   * background thread running.
   */
  public void spawnThread(DukeController controller, int check_interval);

  /**
   * Returns true iff the background thread is running.
   */ 
  public boolean isRunning();
  
  /**
   * Stops the background thread. It can be restarted with a new call
   * to spawnThread.
   */
  public void stop();
  
}