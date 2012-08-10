
package no.priv.garshol.duke.server;

import java.util.Date;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.text.SimpleDateFormat;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import no.priv.garshol.duke.Duke;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.utils.ObjectUtils;

// we use this to make it easier to deal with properties
import static no.priv.garshol.duke.utils.PropertyUtils.get;

/**
 * Starts up Duke processing, and provides a web interface containing
 * some minimal information about the status of the service.
 */
public class StatusServlet extends HttpServlet {
  private SimpleDateFormat format;
  private static DukeController controller;
  private static DukeTimer timer;
  private int check_interval; // in seconds
  private static String DEFAULT_TIMER =
    "no.priv.garshol.duke.server.BasicTimer";
  
  public StatusServlet() {
    this.format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    // load properties
    Properties props = loadPropertiesFromClassPath("duke.properties");
    if (props == null)
      throw new DukeException("Cannot find 'duke.properties' on classpath");

    check_interval = Integer.parseInt(get(props, "duke.check-interval"));
    
    // instantiate main objects
    this.controller = new DukeController(props);

    String val = get(props, "duke.timer-implementation", DEFAULT_TIMER);
    this.timer = (DukeTimer) ObjectUtils.instantiate(val);
    timer.init(props);
        
    // start thread automatically if configured to do so
    String autostart = get(props, "duke.autostart", "false");
    if (autostart.trim().equalsIgnoreCase("true"))
      timer.spawnThread(controller, check_interval);
  }
  
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (req.getParameter("nagios") != null) {
      doNagios(req, resp);
      return;
    }
    
    resp.setContentType("text/html");
    PrintWriter out = resp.getWriter();

    out.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ");
    out.write("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
    out.write("<html xmlns='http://www.w3.org/1999/xhtml'>\n");
    out.write("<head>");
    out.write("<title>DukeThread status</title>");
    out.write("</head>");
    out.write("<body>");
    out.write("<h1>DukeThread status</h1>");

    out.write("<table>");
    out.write("<tr><td>Status: </td><td>" + controller.getStatus() +
              "</td></tr>");
    out.write("<tr><td>Last check at: </td><td>" +
              format(controller.getLastCheck()) +
              "</td></tr>");
    out.write("<tr><td>Last new record at: </td><td>" +
              format(controller.getLastRecord()) + "</td></tr>");
    out.write("<tr><td>Records processed: </td><td>" +
              controller.getRecordCount() +
              "</td></tr>");
    out.write("</table>");

    out.write("<p></p><form method='post' action=''>");
    if (timer.isRunning())
      out.write("<input type='submit' name='stop' value='Stop'/>");
    else
      out.write("<input type='submit' name='start' value='Start'/>");
    out.write("</form>");

    out.write("<p>Duke version " + Duke.getVersionString() + "</p>");
    out.write("</body></html>");
  }

  private void doNagios(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (controller == null) {
      resp.sendError(500, "No controller; Duke not running");
      return;
    }
    if (controller.isErrorBlocked()) {
      resp.sendError(500, controller.getStatus());
      return;
    }

    PrintWriter out = resp.getWriter();
    out.write(controller.getStatus() + ", last check: " +
              format(controller.getLastCheck()) + ", last record: " +
              format(controller.getLastRecord()) + ", records: " +
              controller.getRecordCount());
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (req.getParameter("start") != null)
      timer.spawnThread(controller, check_interval);
    else
      timer.stop();

    resp.sendRedirect("");
  }

  private String format(long time) {
    return format.format(new Date(time));
  }

  public void destroy() {
    try {
      if (controller != null)
        controller.close();
      if (timer != null)
        timer.stop();
    } catch (Exception e) {
      throw new DukeException(e);
    }
  }

  private static Properties loadPropertiesFromClassPath(String name) {
    ClassLoader cloader = Thread.currentThread().getContextClassLoader();
    Properties properties = new Properties();
    InputStream istream = cloader.getResourceAsStream(name);
    if (istream == null)
      return null;
    try {
      properties.load(istream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return properties;
  }
  
}