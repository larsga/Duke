
package no.priv.garshol.duke.sdshare;

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

public class StatusServlet extends HttpServlet {
  private SimpleDateFormat format;
  private static DukeThread duke;

  public StatusServlet() {
    this.format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    Properties props = loadPropertiesFromClassPath("duke.properties");
    
    duke = new DukeThread((String) props.get("duke.configfile"),
                          (String) props.get("duke.linkjdbcuri"));
    
    String val = (String) props.get("duke.batch-size");
    if (val != null)
      duke.setBatchSize(Integer.parseInt(val.trim()));

    val = (String) props.get("duke.sleep-interval");
    if (val != null)
      duke.setSleepInterval(Integer.parseInt(val.trim()));

    // start thread automatically if configured to do so
    String autostart = config.getInitParameter("autostart");
    if (autostart != null && autostart.trim().equalsIgnoreCase("true"))
      duke.start();
  }
  
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    resp.setContentType("text/html");
    PrintWriter out = resp.getWriter();
    
    out.write("<title>DukeThread status</title>");
    out.write("<h1>DukeThread status</h1>");

    out.write("<table>");
    out.write("<tr><td>Status: <td >" + duke.getStatus());
    out.write("<tr><td>Last check at: <td >" + format(duke.getLastCheck()));
    out.write("<tr><td>Last new record at: <td>" + format(duke.getLastRecord()));
    out.write("<tr><td>Records processed: <td>" + duke.getRecords());
    out.write("</table>");

    out.write("<p><form method=post action=''>");
    if (duke.getStopped())
      out.write("<input type=submit name=start value='Start'>");
    else
      out.write("<input type=submit name=stop value='Stop'>");
    out.write("</form>");
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    if (req.getParameter("start") != null)
      duke.start();
    else
      duke.pause();

    resp.sendRedirect("");
  }

  private String format(long time) {
    return format.format(new Date(time));
  }

  public void destroy() {
    if (duke != null)
      duke.close();
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