
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Collection;
import java.util.Collections;
import java.sql.Driver;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;

public class JDBCDataSource implements DataSource {
  private Map<String, Column> columns;
  private String jdbcuri;
  private String driverclass;
  private String username;
  private String password;
  private String query;

  public JDBCDataSource() {
    this.columns = new HashMap();
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
  
  public void addColumn(Column column) {
    columns.put(column.getName(), column);
  }
  
  public Iterator<Record> getRecords() {
    try {
      Class driverclass = Class.forName(this.driverclass);
      Driver driver = (Driver) driverclass.newInstance();
      Properties props = new Properties();
      props.put("user", username);
      props.put("password", password);
      Connection conn = driver.connect(jdbcuri, props);
      Statement stmt = conn.createStatement();

      ResultSet rs = stmt.executeQuery(query);
      return new JDBCIterator(rs);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  class JDBCIterator implements Iterator<Record> {
    private ResultSet rs;
    private boolean next;

    public JDBCIterator(ResultSet rs) throws SQLException {
      this.rs = rs;
      this.next = rs.next();
    }
    
    public boolean hasNext() {
      return next;
    }

    public Record next() {
      try {
        Map<String, Collection<String>> values = new HashMap();
        for (Column col : columns.values()) {
          String value = rs.getString(col.getName());
          if (value == null)
            continue;
          
          if (col.getCleaner() != null)
            value = col.getCleaner().clean(value);
          if (value == null || value.equals(""))
            continue; // nothing here, move on
          if (col.getPrefix() != null)
            value = col.getPrefix() + value;
          
          String propname = col.getProperty();
          values.put(propname, Collections.singleton(value));          
        }

        next = rs.next(); // step to next

        return new RecordImpl(values);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
