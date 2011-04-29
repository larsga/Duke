
package no.priv.garshol.duke;

import java.util.Properties;
import java.util.Collection;

public class JDBCLinkDatabase implements LinkDatabase {
  private String driverklass;
  private String jdbcuri;
  private Properties props;
  
  public JDBCLinkDatabase(String driverklass,
                          String jdbcuri,
                          Properties props) {
    this.driverklass = driverklass;
    this.jdbcuri = jdbcuri;
    this.props = props;

    connect();
    verifySchema();
  }
  
  public Collection<Link> getChangesSince(long since) {
    // simple SQL query. no problems
    return null;
  }

  public Collection<Link> getAllLinks() {
    // simple SQL query. no problems
    return null;
  }

  public void assertLink(Link link) {
    // (1) query to see if the link is already there
    //   if it is, check to see if this one overrides existing one
    //   if not, done

    // (2) write link to database

    // FIXME: consider: what is the transaction boundary here? should we
    // commit to queue db & link db at same time?
  }

  private void connect() {
  }

  private void verifySchema() {
  }
  
}