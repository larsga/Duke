
package no.priv.garshol.duke.utils;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import no.priv.garshol.duke.DukeException;

/**
 * Utilities for making life with JDBC easier.
 */
public class JDBCUtils {

  /**
   * Get a configured database connection via JNDI.
   */
  public static Statement open(String jndiPath) {
    try {
      Context ctx = new InitialContext();
      DataSource ds = (DataSource) ctx.lookup(jndiPath);
      Connection conn = ds.getConnection();
      return conn.createStatement();
    } catch (NamingException e) {
      throw new DukeException("No database configuration found via JNDI at " +
                              jndiPath, e);
    } catch (SQLException e) {
      throw new DukeException("Error connecting to database via " +
                              jndiPath, e);
    }
  }

  /**
   * Opens a JDBC connection with the given parameters.
   */
  public static Statement open(String driverklass, String jdbcuri,
                               Properties props) {
    try {
      Driver driver = (Driver) ObjectUtils.instantiate(driverklass);
      Connection conn = driver.connect(jdbcuri, props);
      if (conn == null)
        throw new DukeException("Couldn't connect to database at " +
                                   jdbcuri);
      return conn.createStatement();

    } catch (SQLException e) {
      throw new DukeException(e);
    }
  }

  /**
   * Closes the JDBC statement and its associated connection.
   */
  public static void close(Statement stmt) {
    try {
      Connection conn = stmt.getConnection();
      try {
        if (!stmt.isClosed())
          stmt.close();
      } catch (UnsupportedOperationException e) {
        // not all JDBC drivers implement the isClosed() method.
        // ugly, but probably the only way to get around this.
        // http://stackoverflow.com/questions/12845385/duke-fast-deduplication-java-lang-unsupportedoperationexception-operation-not
        stmt.close();
      }
      if (conn != null && !conn.isClosed())
        conn.close();
    } catch (SQLException e) {
      throw new DukeException(e);
    }
  }

  /**
   * Verifies that the connection is still alive. Returns true if it
   * is, false if it is not. If the connection is broken we try
   * closing everything, too, so that the caller need only open a new
   * connection.
   */
  public static boolean validate(Statement stmt) {
    try {
      Connection conn = stmt.getConnection();
      if (conn == null)
        return false;

      if (!conn.isClosed() && conn.isValid(10))
        return true;

      stmt.close();
      conn.close();
    } catch (SQLException e) {
      // this may well fail. that doesn't matter. we're just making an
      // attempt to clean up, and if we can't, that's just too bad.
    }
    return false;
  }

  /**
   * Runs a query that returns a single int.
   */
  public static int queryForInt(Statement stmt, String sql, int nullvalue) {
    try {
      ResultSet rs = stmt.executeQuery(sql);
      try {
        if (!rs.next())
          return nullvalue;
        
        return rs.getInt(1);
      } finally {
        rs.close();
      }
    } catch (SQLException e) {
      throw new DukeException(e);
    }
  }

  /**
   * Returns true if the query result has at least one row.
   */
  public static boolean queryHasResult(Statement stmt, String sql) {
    try {
      ResultSet rs = stmt.executeQuery(sql);
      try {
        return rs.next();
      } finally {
        rs.close();
      }
    } catch (SQLException e) {
      throw new DukeException(e);
    }
  }
}