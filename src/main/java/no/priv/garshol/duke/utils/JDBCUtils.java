
package no.priv.garshol.duke.utils;

import java.util.Properties;
import java.sql.Driver;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Utilities for making life with JDBC easier.
 */
public class JDBCUtils {

  /**
   * get a configured database connection via JNDI
   */
  public static Statement open(String jndiPath) {
    try {
      Context ctx = new InitialContext();
      DataSource ds = (DataSource) ctx.lookup(jndiPath);
      Connection conn = ds.getConnection();
      return conn.createStatement();
    } catch (NamingException e) {
      throw new RuntimeException("No database configuration found via JNDI at "
                                 + jndiPath, e);
    } catch (SQLException e) {
      throw new RuntimeException("Error connecting to database via " +
                                 jndiPath, e);
    }
  }
	
  public static Statement open(String driverklass, String jdbcuri,
                               Properties props) {
    try {
      Driver driver = (Driver) ObjectUtils.instantiate(driverklass);
      Connection conn = driver.connect(jdbcuri, props);
      if (conn == null)
        throw new RuntimeException("Couldn't connect to database at " +
                                   jdbcuri);
      return conn.createStatement();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void close(Statement stmt) {
    try {
      Connection conn = stmt.getConnection();
      if (!stmt.isClosed())
        stmt.close();
      if (!conn.isClosed())
        conn.close();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void close(ResultSet rs) {
    try {
      Statement stmt = rs.getStatement();
      if (!rs.isClosed())
        rs.close();
      JDBCUtils.close(stmt);
    } catch (SQLException e) {
      throw new RuntimeException(e);
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
}