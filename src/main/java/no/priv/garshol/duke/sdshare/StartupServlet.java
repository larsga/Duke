
package no.priv.garshol.duke.sdshare;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
  
public class StartupServlet extends HttpServlet {
  private static DukeThread duke;
  
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    start();
  }

  public static void start() {
    Properties props = loadPropertiesFromClassPath("duke.properties");
    
    duke = new DukeThread((String) props.get("duke.configfile"),
                          (String) props.get("duke.linkjdbcuri"));

    String val = (String) props.get("duke.batch-size");
    if (val != null)
      duke.setBatchSize(Integer.parseInt(val.trim()));
    
    duke.start();
  }

  public static DukeThread getThread() {
    return duke;
  }

  private static Properties loadPropertiesFromClassPath(String name) {
    ClassLoader cloader = StartupServlet.class.getClassLoader();
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