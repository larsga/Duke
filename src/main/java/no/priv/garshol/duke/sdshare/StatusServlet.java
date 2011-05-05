
package no.priv.garshol.duke.sdshare;

import java.util.Date;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StatusServlet extends HttpServlet {
  private SimpleDateFormat format;

  public StatusServlet() {
    this.format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    resp.setContentType("text/html");
    PrintWriter out = resp.getWriter();
    DukeThread duke = StartupServlet.getThread();
    
    out.write("<title>DukeThread status</title>");
    out.write("<h1>DukeThread status</h1>");

    out.write("<table>");
    out.write("<tr><td>Status: <td >" + duke.getStatus());
    out.write("<tr><td>Last check at: <td >" + format(duke.getLastCheck()));
    out.write("<tr><td>Last new record at: <td>" + format(duke.getLastRecord()));
    out.write("<tr><td>Records processed: <td>" + duke.getRecords());
    out.write("</table>");

    if (duke.getStopped()) {
      out.write("<p><form method=post action=''>");
      out.write("<input type=submit name=start value='Start'>");
      out.write("</form>");
    }
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    StartupServlet.start();
    
  }

  private String format(long time) {
    return format.format(new Date(time));
  }
  
}