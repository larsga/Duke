
package no.priv.garshol.duke;

import java.util.Properties;

import no.priv.garshol.duke.utils.JDBCUtils;

/**
 * A link database which can maintain a set of links in an H2 or
 * Oracle database over JDBC. It could be extended to work with more
 * database implementations.
 */
public class JDBCLinkDatabase extends RDBMSLinkDatabase {
  private String driverklass;
  private String jdbcuri;
  private Properties props;
  
  public JDBCLinkDatabase(String driverklass,
                          String jdbcuri,
                          String dbtype,
                          Properties props) {
    super(dbtype);
    this.driverklass = driverklass;
    this.jdbcuri = jdbcuri;
    this.props = props;
    this.stmt = JDBCUtils.open(driverklass, jdbcuri, props);
  }

  public void validateConnection() {
    if (stmt != null && !JDBCUtils.validate(stmt))
      // it failed to validate, and was closed by the validate method.
      // we therefore reopen so that we have a proper connection.
      stmt = JDBCUtils.open(driverklass, jdbcuri, props);
  }
    
}