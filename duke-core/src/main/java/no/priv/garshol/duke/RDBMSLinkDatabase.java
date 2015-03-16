
package no.priv.garshol.duke;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.priv.garshol.duke.utils.JDBCUtils;

/**
 * An abstract SQL-based link database implementation which can can
 * maintain a set of links in an H2 or Oracle database over JDBC. It
 * could be extended to work with more database implementations. What
 * the abstract class cannot do is create a connection, which is left
 * for subclasses to do.
 */
public abstract class RDBMSLinkDatabase implements LinkDatabase {
  private DatabaseType dbtype;
  private String tblprefix; // prefix for table names ("foo."); never null
  protected Statement stmt; // set by subclass
  private Logger logger;
  private static final SimpleDateFormat dtformat =
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  
  public RDBMSLinkDatabase(String dbtype) {
    this.dbtype = getDatabaseType(dbtype);
    this.tblprefix = "";
    this.logger = new DummyLogger();
  }

  /**
   * This method must be called to initialize the database. In order
   * to avoid problems with constructor call sequencing (parent
   * constructor must be called first), we had to move this out into a
   * separate method.
   */
  public void init() {
    try {
      verifySchema();
    } catch (Throwable e) {
      close();
      throw new DukeException(e);
    }
  }
  
  public void setLogger(Logger logger) {
    this.logger = logger;
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

    // oracle must do the limit in where, while H2 supports normal SQL
    String limit = "";
    if (pagesize != 0) {
      limit = dbtype.getLimit(pagesize);
      if (limit.length() == 0) {
        // we are in oracle territory now. prepare for some seriously ugly
        // string hacking
        if (where.length() > 0) // hack hack
          where += " AND " + dbtype.getWhereLimit(pagesize);
        else // hackety hackty
          where += " where " + dbtype.getWhereLimit(pagesize);
        // *vomit*
      }
    }
    
    return queryForLinks("select * from " + tblprefix + "links " + where +
                         " order by timestamp desc " + limit);
  }
  
  public Collection<Link> getAllLinksFor(String id) {
    return queryForLinks("select * from " + tblprefix + "links where " +
                         "id1 = '" + escape(id) + "' or " +
                         "id2 = '" + escape(id) + "'");
  }

  public void assertLink(Link link) {
    logger.debug("Asserting link " + link);
    
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
      if (existing != null) {
        logger.trace("Updating link for " + link.getID1() + " and " +
                     link.getID2());
        query = "update " + tblprefix + "links set status = " +
          link.getStatus().getId() +
          " , kind = " + link.getKind().getId() + 
          " , timestamp = " + dbtype.getNow() + " " +
          " , confidence = " + link.getConfidence() + " " +
          "where id1 = '" + escape(link.getID1()) + "' " +
          "      and id2 = '" + escape(link.getID2()) + "' ";
      } else {
        logger.trace("Inserting link for " + link.getID1() + " and " +
                     link.getID2());
        query = "insert into " + tblprefix + "links values ('" + escape(link.getID1()) + "', " +
          "  '" + escape(link.getID2()) + "', " + link.getKind().getId() +
          "  , " + link.getStatus().getId() + ", " + dbtype.getNow() +
          ", " + link.getConfidence() + ") ";
      }
      stmt.executeUpdate(query);
      
    } catch (SQLException e) {
      close(); // releasing connection
      throw new DukeException(e);
    }
  }
  
  public Link inferLink(String id1, String id2) {
    // are we sure this method really belongs in the interface, and
    // not in an external utility?
    throw new DukeException("not implemented yet");
  }
  
  /**
   * Empties the link database. Used only for testing at the moment.
   */
  public void clear() {
    try {
      stmt.executeUpdate("delete from " + tblprefix + "links");
    } catch (SQLException e) {
      close(); // releasing connection
      throw new DukeException(e);
    }
  }

  public void commit() {
    try {
      Connection conn = stmt.getConnection();
      if (!conn.getAutoCommit())
        // we only call commit if the connection is not auto-committing, as
        // mysql throws an exception otherwise (issue 105)
        conn.commit();
    } catch (SQLException e) {
      close(); // releasing connection
      throw new DukeException(e);
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

    boolean present = JDBCUtils.queryHasResult(stmt,
       "select * from " + dbtype.getMetaTableName() + " " +
       "where table_name = 'LINKS'" + lastpart);
    if (present)
      return;

    logger.warn("Table LINKS not found; recreating");
    stmt.executeUpdate(dbtype.getCreateTable());

    // creating indexes, too, as that makes processing *much* faster
    stmt.executeUpdate("create index " + tblprefix + "links_ix_id1 on " +
                       tblprefix + "links (id1)");
    stmt.executeUpdate("create index " + tblprefix + "links_ix_id2 on " +
                       tblprefix + "links (id2)");
  }

  private String escape(String strval) {
    return strval.replace("'", "''");
  }

  private List<Link> queryForLinks(String query) {
    List<Link> links = new ArrayList();
    
    try {
      logger.trace("Querying for links: " + query);
      ResultSet rs = stmt.executeQuery(query);
      while (rs.next())
        links.add(makeLink(rs));
      rs.close(); // FIXME: finally
    } catch (SQLException e) {
      close(); // releasing connection
      throw new DukeException(e);
    }

    return links;
  }

  private Link makeLink(ResultSet rs) throws SQLException {
    return new Link(rs.getString("id1"),
                    rs.getString("id2"),
                    LinkStatus.getbyid(rs.getInt("status")),
                    LinkKind.getbyid(rs.getInt("kind")),
                    rs.getTimestamp("timestamp").getTime(),
                    rs.getDouble("confidence"));
  }

  // ===== DATABASE TYPES

  private static DatabaseType getDatabaseType(String dbtype) {
    if (dbtype.equals("h2"))
      return DatabaseType.H2;
    else if (dbtype.equals("oracle"))
      return DatabaseType.ORACLE;
    else if (dbtype.equals("mysql"))
      return DatabaseType.MYSQL;
    else
      throw new DukeConfigException("Unknown database type: '" + dbtype + "'");
  }
  
  public enum DatabaseType {
    MYSQL {
      public String getMetaTableName() {
        return "information_schema.tables";
      }

      public String getCreateTable() {
        return "create table LINKS ( " +
               "  id1 varchar (100) not null, " +
               "  id2 varchar (100) not null, " +
               "  kind int not null, " +
               "  status int not null, " +
               "  timestamp timestamp not null, " +
               "  confidence float not null, " +
               "  primary key (id1, id2)) ";
      }

      public String getNow() {
        return "now()";
      }

      public String getLimit(int no) {
        return "limit " + no;
      }

      public String getWhereLimit(int no) {
        return "";
      }
    },

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
               "  confidence float not null, " +
               "  primary key (id1, id2)) ";
      }

      public String getNow() {
        return "now()";
      }

      public String getLimit(int no) {
        return "limit " + no;
      }

      public String getWhereLimit(int no) {
        return "";
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
               "  confidence float not null, " +
               "  primary key (id1, id2)) ";
      }

      public String getNow() {
        return "current_timestamp";
      }

      public String getLimit(int no) {
        return "";
      }

      public String getWhereLimit(int no) {
        return "rownum <= " + no;
      }
    };

    public abstract String getMetaTableName();
    public abstract String getCreateTable();
    public abstract String getNow();
    public abstract String getLimit(int no);
    public abstract String getWhereLimit(int no);
  }
  
}