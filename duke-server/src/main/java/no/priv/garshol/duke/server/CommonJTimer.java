
package no.priv.garshol.duke.server;

import java.util.Properties;
import javax.naming.InitialContext;

import commonj.timers.Timer;
import commonj.timers.TimerManager;
import commonj.timers.TimerListener;

import no.priv.garshol.duke.DukeException;
  
// makes it easier to deal with properties
import static no.priv.garshol.duke.utils.PropertyUtils.get;
  
/**
 * Timer implementation which uses the JSR-236 API, in order to
 * provide managed threads within servlet containers that support
 * them.
 */
public class CommonJTimer implements DukeTimer, TimerListener {
  private TimerManager mgr;
  private Timer timer;
  private DukeController controller;

  public CommonJTimer() {
  }

  public void init(Properties props) {
    String path = get(props, "duke.timer-jndipath");
    try {
      InitialContext ctx = new InitialContext();
      mgr = (TimerManager) ctx.lookup(path);
    }
    catch (Exception e) {
      throw new DukeException(e);
    }
  }
  
  /**
   * Starts a background thread which calls the controller every
   * check_interval milliseconds. Returns immediately, leaving the
   * background thread running.
   */
  public void spawnThread(DukeController controller, int check_interval) {
    this.controller = controller;
    timer = mgr.schedule(this, 0, check_interval * 1000); // convert to ms
  }

  /**
   * Returns true iff the background thread is running.
   */ 
  public boolean isRunning() {
    return timer != null;
  }
  
  /**
   * Stops the background thread. It can be restarted with a new call
   * to spawnThread.
   */
  public void stop() {
    timer.cancel();
    timer = null;
  }

  /**
   * This is the callback from the timer service, letting us know it's
   * time do something.
   */
  public void timerExpired(Timer timer) {
    controller.process();
  }
}