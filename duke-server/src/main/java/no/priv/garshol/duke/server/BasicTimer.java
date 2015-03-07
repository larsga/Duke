
package no.priv.garshol.duke.server;

import java.util.Properties;

import no.priv.garshol.duke.DukeException;

/**
 * A basic timer implementation that will work in any context, but
 * which does not (unfortunately) provide managed threads in more
 * advanced servlet containers. For that, look at CommonJTimer.
 */
public class BasicTimer implements DukeTimer, Runnable {
  private DukeController controller;
  private int check_interval;
  private int sleep_interval;
  private boolean keep_running;
  
  // --- Setup
  
  public BasicTimer() {
    this.sleep_interval = 100; // default
  }

  // --- DukeTimer implementation

  public void init(Properties props) {
    // don't need to do anything
  }
  
  public void spawnThread(DukeController controller, int check_interval) {
    if (this.controller != null)
      throw new DukeException("Timer thread already running!");
    
    this.controller = controller;
    this.check_interval = check_interval * 1000; // convert to ms
    keep_running = true;

    // spawn away
    Thread thread = new Thread(this);
    thread.setDaemon(true);
    thread.start();
  }

  public boolean isRunning() {
    return keep_running;
  }

  public void stop() {
    controller = null;
    keep_running = false;
  }
  
  // --- Runnable implementation

  public void run() {
    while (keep_running) {
      try {
        // tell controller to do some real work for a change
        controller.process();

        // waiting check_interval ms, while taking sleep_interval ms
        // long naps so we can break off faster if the server is shut
        // down
        long wait_start = System.currentTimeMillis();
        do {
          Thread.sleep(sleep_interval);
        } while (keep_running &&
                 (System.currentTimeMillis() - wait_start) < check_interval);

      } catch (Throwable e) {
        controller.reportError(e);
        try {
          Thread.sleep(getErrorWaitInteral()); // wait a good while, then retry
        } catch (InterruptedException e2) {
        }
      }
    }
    controller.reportStopped();
  }

  // --- Internal methods

  private int getErrorWaitInteral() {
    return check_interval * 6;
  }
  
}