
package no.priv.garshol.duke.sdshare;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collection;
import java.util.Collections;
import java.sql.Driver;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;

import no.priv.garshol.duke.Column;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.DataSource;
import no.priv.garshol.duke.SparqlClient;
import no.priv.garshol.duke.RecordIterator;

/**
 * Data source which produces records by polling a queue in an H2
 * database.
 */
public class SDshareDataSource implements DataSource {
  private Map<String, Column> columns;
  private String jdbcuri;
  private String endpoint;

  public SDshareDataSource() {
    this.columns = new HashMap();
  }
  
  public void addColumn(Column column) {
    columns.put(column.getName(), column);
  }
  
  public RecordIterator getRecords() {
    try {
      Class driverclass = Class.forName("org.h2.Driver");
      Driver driver = (Driver) driverclass.newInstance();
      Properties props = new Properties();
      Connection conn = driver.connect(jdbcuri, props);
      Statement stmt = conn.createStatement();

      return new SDshareIterator(stmt);
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

  class SDshareIterator extends RecordIterator {
    private Statement stmt;
    private ResultSet rs;
    private boolean next;
    private int previd; // id of the previous record we returned
    
    public SDshareIterator(Statement stmt) throws SQLException {
      this.stmt = stmt;
      this.rs = stmt.executeQuery("select * from UPDATED_RESOURCES " +
                                  "order by id asc");
      this.next = rs.next();
    }
    
    public boolean hasNext() {
      return next;
    }

    public Record next() {
      try {
        String resource = rs.getString("uri");
        
        Column uricol = columns.get("?uri");

        List<String[]> data = getProperties(resource);
        Map<String, Collection<String>> record = new HashMap();
        record.put(uricol.getProperty(), Collections.singleton(resource));

        for (String[] row : data) {
          String value = row[1];
          Column col = columns.get(row[0]);
          if (col == null)
            continue;

          if (value == null)
            continue;
          if (col.getCleaner() != null)
            value = col.getCleaner().clean(value);
          if (value == null || value.equals(""))
            continue; // nothing here, move on
        
          String prop = col.getProperty();
          Collection<String> values = record.get(prop);
          if (values == null) {
            values = new ArrayList();
            record.put(prop, values);
          }
          values.add(value);
        }

        next = rs.next();
        previd = rs.getInt("id");
        return new RecordImpl(record);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    
    public void remove() {
      throw new UnsupportedOperationException();
    }

    public void close() {
      try {
        stmt.executeUpdate("delete from UPDATED_RESOURCES where id <= " + previd);
        rs.close();
        stmt.close();
        stmt.getConnection().close();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    private List<String[]> getProperties(String resource) {
      return SparqlClient.execute(endpoint,
                                  "select ?p ?o where { <" +
                                  resource + "> ?p ?o }");
    }
  }
}
