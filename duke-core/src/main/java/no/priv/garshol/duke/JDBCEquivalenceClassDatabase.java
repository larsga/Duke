
package no.priv.garshol.duke;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import no.priv.garshol.duke.utils.JDBCUtils;

/**
 * An eq. class database using an RDBMS as backing.
 * @since 1.0
 */
public class JDBCEquivalenceClassDatabase implements EquivalenceClassDatabase {
  private Statement stmt; // set by subclass
  private int nextclassid;
  
  public JDBCEquivalenceClassDatabase(String driverklass,
                                      String jdbcuri,
                                      String dbtype,
                                      Properties props) {
    this.stmt = JDBCUtils.open(driverklass, jdbcuri, props);
    init();
    this.nextclassid = getNextClassId();
  }
  
  public int getClassCount() {
    throw new UnsupportedOperationException();
  }

  public Iterator<Collection<String>> getClasses() {
    throw new UnsupportedOperationException();
  }
  
  public Collection<String> getClass(String id) {
    int clid = JDBCUtils.queryForInt(stmt, "select clid from classes " +
                                     "where id = '" + id + "'", -1);
    
    List ids = new ArrayList();
    try {
      ResultSet rs = stmt.executeQuery("select id from classes where clid = " +
                                       clid);
      try {
        while (rs.next())
          ids.add(rs.getString(1));
      } finally {
        rs.close();
      }
    } catch (SQLException e) {
      throw new DukeException(e);
    }
    return ids;
  }
  
  public void addLink(String id1, String id2) {
    int clid1 = getClassId(id1);
    int clid2 = getClassId(id2);

    if (clid1 == clid2 && clid1 != -1)
      return; // we already knew

    if (clid1 == -1 && clid2 == -1) {
      // don't know these from before, so make a new class for them
      addToClass(id1, nextclassid);
      addToClass(id2, nextclassid);
      nextclassid++;
    } else if ((clid1 == -1 && clid2 != -1) ||
               (clid1 != -1 && clid2 == -1)) {
      // one of these has no class, so we add it to the class of the other
      if (clid1 == -1)
        addToClass(id1, clid2);
      else
        addToClass(id2, clid1);
    } else
      // we have classes for both, but they're different
      merge(clid1, clid2);
  }
  
  public void commit() {
    // mysql is autocommiting, so no need
  }

  public int getClassId(String id) {
    return JDBCUtils.queryForInt(stmt, "select clid from classes " +
                                 "where id = '" + id + "'", -1);
  }

  private void addToClass(String id, int clid) {
    try {
      stmt.executeUpdate("insert into classes values ('" + id + "', " + clid + ")");
    } catch (SQLException e) {
      throw new DukeException(e);
    }
  }

  private void merge(int clid1, int clid2) {
    try {
      stmt.executeUpdate("update classes set clid = " + clid1 +
                         " where clid = " + clid2);
    } catch (SQLException e) {
      throw new DukeException(e);
    }
  }

  private int getNextClassId() {
    return JDBCUtils.queryForInt(stmt, "select max(clid) from classes", 0) + 1;
  }

  private void init() {
    if (JDBCUtils.queryHasResult(stmt,
                                 "select * from information_schema.tables " +
                                 "where table_name = 'CLASSES'"))
      return; // table exists, no problem

    try {
      // this table contains the equivalence classes. 'clid' has the
      // ID of the class, and 'id' the ID of the record that has been
      // put into the class. each 'id' should occur only once, while
      // the 'clid's will occur once for each record in the class.
      stmt.executeUpdate("create table classes (id varchar(100) not null, " +
                         "                      clid int not null, " +
                         "                      primary key (id, clid))");
    } catch (SQLException e) {
      throw new DukeException(e);
    }
  }
}