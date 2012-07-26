
package no.priv.garshol.duke;

import no.priv.garshol.duke.utils.JDBCUtils;

/**
 * A link database that gets its connection via JNDI lookup.
 */
public class JNDILinkDatabase extends RDBMSLinkDatabase {
  
  public JNDILinkDatabase(String jndipath, String dbtype) {
    super(dbtype);
    this.stmt = JDBCUtils.open(jndipath);
  }
  
}