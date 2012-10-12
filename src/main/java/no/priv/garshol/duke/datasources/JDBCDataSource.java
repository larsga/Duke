
package no.priv.garshol.duke.datasources;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Collection;
import java.util.Collections;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import no.priv.garshol.duke.Column;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.utils.JDBCUtils;

public class JDBCDataSource extends ColumnarDataSource {
  private String jdbcuri;
  private String driverclass;
  private String username;
  private String password;
  private String query;

  public JDBCDataSource() {
    super();
  }

  public void setConnectionString(String str) {
    this.jdbcuri = str;
  }

  public void setDriverClass(String klass) {
    this.driverclass = klass;
  }

  public void setUserName(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getQuery() {
    return this.query;
  }
  
  public RecordIterator getRecords() {
    verifyProperty(jdbcuri, "connection-string");
    verifyProperty(driverclass, "driver-class");
    verifyProperty(query, "query");
    
    try {
      Properties props = new Properties();
      if (username != null)
        props.put("user", username);
      if (password != null)
        props.put("password", password);
      Statement stmt = JDBCUtils.open(driverclass, jdbcuri, props);
      ResultSet rs = stmt.executeQuery(query);
      return new JDBCIterator(rs);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  protected String getSourceName() {
    return "JDBC";
  }

  public class JDBCIterator extends RecordIterator {
    private Statement stmt;
    private ResultSet rs;
    private boolean next;
    private RecordBuilder builder;

    public JDBCIterator(ResultSet rs) throws SQLException {
      this.rs = rs;
      this.next = rs.next();
      // can't call rs.getStatement() after rs is closed, so must do it now
      this.stmt = rs.getStatement();
      this.builder = new RecordBuilder(JDBCDataSource.this);
    }
    
    public boolean hasNext() {
      return next;
    }

    public Record next() {
      try {
        builder.newRecord();
        for (Column col : getColumns()) {
          String value = rs.getString(col.getName());
          builder.addValue(col, value);
        }

        next = rs.next(); // step to next

        return builder.getRecord();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    public void close() {
      try {
        try {
          if (!rs.isClosed())
            rs.close();
        } catch (UnsupportedOperationException e) {
          // not all JDBC drivers implement the isClosed() method.
          // ugly, but probably the only way to get around this.
          // http://stackoverflow.com/questions/12845385/duke-fast-deduplication-java-lang-unsupportedoperationexception-operation-not
          rs.close();
        }
      } catch (SQLException e) {
        throw new DukeException(e);
      }
      JDBCUtils.close(stmt);
    }
  }
}
