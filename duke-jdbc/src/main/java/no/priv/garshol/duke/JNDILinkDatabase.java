
package no.priv.garshol.duke;

import no.priv.garshol.duke.utils.JDBCUtils;

/**
 * A link database that gets its connection via JNDI lookup.
 */
public class JNDILinkDatabase extends RDBMSLinkDatabase {
  private String jndipath;
  
  public JNDILinkDatabase(String jndipath, String dbtype) {
    super(dbtype);
    this.jndipath = jndipath;
    this.stmt = JDBCUtils.open(jndipath);
  }

  public void validateConnection() {
    if (stmt != null && !JDBCUtils.validate(stmt))
      stmt = JDBCUtils.open(jndipath);
  }
  
}