
package no.priv.garshol.duke;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collection;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import no.priv.garshol.duke.utils.JDBCUtils;

/**
 * A link database which can maintain a set of links in an H2 database
 * over JDBC. Could be improved to work with other databases, too, but
 * haven't tried that yet.
 */
public class JDBCLinkDatabase implements LinkDatabase {
  private DatabaseType dbtype;
  private String driverklass;
  private String jdbcuri;
  private String tblprefix; // prefix for table names ("foo."); never null
  private Properties props;
  private Statement stmt;
  private static final SimpleDateFormat dtformat =
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  
  public JDBCLinkDatabase(String driverklass,
                          String jdbcuri,
                          String dbtype,
                          Properties props) {
    this.driverklass = driverklass;
    this.jdbcuri = jdbcuri;
    this.props = props;
    this.dbtype = getDatabaseType(dbtype);
    this.stmt = JDBCUtils.open(driverklass, jdbcuri, props);
    this.tblprefix = "";

    try {
      verifySchema();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void setTablePrefix(String tblprefix) {
    this.tblprefix = tblprefix;
  }
  
  public List<Link> getAllLinks() {
    return getChangesSince(0, 0, 0);
  }
  
  public List<Link> getChangesSince(long since) {
    return getChangesSince(since, 0, 0);
  }

  public List<Link> getChangesSince(long since, long before) {
    return getChangesSince(since, before, 0);
  }

  public List<Link> getChangesSince(long since, long before, int pagesize) {
    String where = "";
    if (since != 0 || before != 0)
      where = "where ";
    if (since != 0)
      where += "timestamp > TIMESTAMP '" + dtformat.format(since) + "'";
    if (before != 0) {
      if (since != 0)
        where += "and ";
      where += "timestamp <= TIMESTAMP '" + dtformat.format(before) + "'";
    }

    String limit = "";
    if (pagesize != 0)
      limit = " limit " + pagesize;
    
    return queryForLinks("select * from " + tblprefix + "links " + where +
                         " order by timestamp desc" + limit);
  }
  
  public Collection<Link> getAllLinksFor(String id) {
    return queryForLinks("select * from " + tblprefix + "links where " +
                         "id1 = '" + escape(id) + "' or " +
                         "id2 = '" + escape(id) + "'");
  }

  public void assertLink(Link link) {
    // (1) query to see if the link is already there
    // FIXME: use prepared statement
    try {
      Link existing = null;
      ResultSet rs = stmt.executeQuery("select * from " + tblprefix +
                                       "links where " +
                                  "id1 = '" + escape(link.getID1()) + "' and " +
                                  "id2 = '" + escape(link.getID2()) + "'");
      if (rs.next()) {
        existing = makeLink(rs);
        rs.close();
        
        if (!link.overrides(existing))
          return; // the existing link rules, so we shut up and go away
      }
      rs.close();
      
      // (2) write link to database
      String query;
      if (existing != null)
        query = "update " + tblprefix + "links set status = " +
          link.getStatus().getId() +
          "  , kind = " + link.getKind().getId() + 
          "  , timestamp = " + dbtype.getNow() + " " +
          "where id1 = '" + escape(link.getID1()) + "' " +
          "      and id2 = '" + escape(link.getID2()) + "' ";
      else
        query = "insert into " + tblprefix + "links values ('" + escape(link.getID1()) + "', " +
          "  '" + escape(link.getID2()) + "', " + link.getKind().getId() +
          "  , " + link.getStatus().getId() + ", now()) ";
      stmt.executeUpdate(query);
      
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Empties the link database. Used only for testing at the moment.
   */
  public void clear() {
    try {
      stmt.executeUpdate("delete from " + tblprefix + "links");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void commit() {
    try {
      stmt.getConnection().commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public void close() {
    JDBCUtils.close(stmt);
  }

  private void verifySchema() throws SQLException {
    String lastpart = "";
    if (!tblprefix.equals(""))
      lastpart = "AND owner = '" +
        tblprefix.substring(0, tblprefix.length() - 1) + "'";
    
    ResultSet rs = stmt.executeQuery("select * from " +
                                     dbtype.getMetaTableName() + " " +
                                     "where table_name = 'LINKS'" + lastpart);
    boolean present = rs.next();
    rs.close();

    if (present)
      return;

    stmt.executeUpdate(dbtype.getCreateTable());
  }

  private String escape(String strval) {
    return strval.replace("'", "''");
  }

  private List<Link> queryForLinks(String query) {
    List<Link> links = new ArrayList();
    
    try {
      ResultSet rs = stmt.executeQuery(query);
      while (rs.next())
        links.add(makeLink(rs));
      rs.close(); // FIXME: finally
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return links;
  }

  private Link makeLink(ResultSet rs) throws SQLException {
    return new Link(rs.getString("id1"),
                    rs.getString("id2"),
                    LinkStatus.getbyid(rs.getInt("status")),
                    LinkKind.getbyid(rs.getInt("kind")),
                    rs.getTimestamp("timestamp").getTime());
  }

  // ===== DATABASE TYPES

  private static DatabaseType getDatabaseType(String dbtype) {
    if (dbtype.equals("h2"))
      return DatabaseType.H2;
    else if (dbtype.equals("oracle"))
      return DatabaseType.ORACLE;
    else
      throw new DukeConfigException("Unknown database type: '" + dbtype + "'");
  }
  
  public enum DatabaseType {
    H2 {
      public String getMetaTableName() {
        return "information_schema.tables";
      }

      public String getCreateTable() {
        return "create table LINKS ( " +
               "  id1 varchar not null, " +
               "  id2 varchar not null, " +
               "  kind int not null, " +
               "  status int not null, " +
               "  timestamp timestamp not null, " +
               "  primary key (id1, id2)) ";
      }

      public String getNow() {
        return "now()";
      }
    },

    ORACLE {
      public String getMetaTableName() {
        return "all_tables";
      }

      public String getCreateTable() {
        return "create table LINKS ( " +
               "  id1 varchar(200) not null, " +
               "  id2 varchar(200) not null, " +
               "  kind int not null, " +
               "  status int not null, " +
               "  timestamp timestamp not null, " +
               "  primary key (id1, id2)) ";
      }

      public String getNow() {
        return "current_timestamp";
      }
    };

    public abstract String getMetaTableName();
    public abstract String getCreateTable();
    public abstract String getNow();
  }
  
}