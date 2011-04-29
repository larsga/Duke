
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
import no.priv.garshol.duke.ColumnarDataSource;
import no.priv.garshol.duke.PrintMatchListener;

/**
 * Data source which produces records by polling a queue in an H2
 * database.
 */
public class SDshareDataSource extends ColumnarDataSource {
  private String jdbcuri;
  private String endpoint;
  private String inverseProperty;

  public SDshareDataSource() {
    super();
  }
  
  public RecordIterator getRecords() {
    try {
      System.out.println("SDshareDataSource.getRecords()");
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

  public void setConnectionString(String str) {
    this.jdbcuri = str;
  }

  public void setEndpoint(String str) {
    this.endpoint = str;
  }

  public void setInverseProperties(String str) {
    // FIXME: should parse into tokens
    this.inverseProperty = str;
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
      this.previd = -1;
      System.out.println("next: " + next);
    }
    
    public boolean hasNext() {
      return next;
    }

    public Record next() {
      try {
        String resource = rs.getString("uri");
        System.out.println("Resource: " + resource);
        
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

        previd = rs.getInt("id");
        next = rs.next();
        RecordImpl r = new RecordImpl(record);
        System.out.println(PrintMatchListener.toString(r));
        return r;
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    
    public void remove() {
      throw new UnsupportedOperationException();
    }

    public void close() {
      try {
        if (previd != -1) {
          System.out.println("delete from UPDATED_RESOURCES where id <= " + previd);
          stmt.executeUpdate("delete from UPDATED_RESOURCES where id <= " + previd);
        }
        rs.close();
        Connection c = stmt.getConnection();
        stmt.close();
        c.close();
      } catch (SQLException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    private List<String[]> getProperties(String resource) {
      StringBuffer query = new StringBuffer();
      query.append("select distinct ?p ?o where { ");
      query.append("  graph ?g { ");
      query.append("  { <" + resource + "> ?p ?o }");
      if (inverseProperty != null) {
        query.append("  union ");
        query.append("  { ?v <" + inverseProperty + "> <" + resource + "> . ");
        query.append("    ?v ?p ?o . } ");
      }
      query.append(" } ");
      query.append("} ");

      System.out.println("query: " + query);
      
      return SparqlClient.execute(endpoint, query.toString());
    }
  }
}
