
package no.priv.garshol.duke.utils;

import java.util.Properties;
import java.sql.Driver;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;

public class JDBCUtils {

  public static Statement open(String driverklass, String jdbcuri, Properties props) {
    try {
      Driver driver = (Driver) ObjectUtils.instantiate(driverklass);
      Connection conn = driver.connect(jdbcuri, props);
      if (conn == null)
        throw new RuntimeException("Couldn't connect to database at " + jdbcuri);
      return conn.createStatement();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void close(Statement stmt) {
    try {
      Connection conn = stmt.getConnection();
      stmt.close();
      conn.close();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void close(ResultSet rs) {
    try {
      Statement stmt = rs.getStatement();
      rs.close();
      JDBCUtils.close(stmt);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}