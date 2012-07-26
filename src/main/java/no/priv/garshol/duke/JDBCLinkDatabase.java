
package no.priv.garshol.duke;

import java.util.Properties;
import no.priv.garshol.duke.utils.JDBCUtils;

/**
 * A link database which can maintain a set of links in an H2 or
 * Oracle database over JDBC. It could be extended to work with more
 * database implementations.
 */
public class JDBCLinkDatabase extends RDBMSLinkDatabase {
  
  public JDBCLinkDatabase(String driverklass,
                          String jdbcuri,
                          String dbtype,
                          Properties props) {
    super(dbtype);
    this.stmt = JDBCUtils.open(driverklass, jdbcuri, props);
  }
  
}