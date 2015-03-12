
package no.priv.garshol.duke.datasources;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Properties;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner;
import no.priv.garshol.duke.cleaners.RegexpCleaner;
import no.priv.garshol.duke.utils.JDBCUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JDBCDataSourceTest {
  private Statement stmt;
  private JDBCDataSource source;
  private static final String DRIVER = "org.h2.Driver";
  private static final String JDBC_URI = "jdbc:h2:mem:testdb";
  
  @Before
  public void setUp() {
    // clear database
    connect();
    perform("drop table if exists testdata");
    perform("create table testdata (id int, name varchar)");

    // create data source
    source = new JDBCDataSource();
    source.setConnectionString(JDBC_URI);
    source.setDriverClass(DRIVER);
    source.setQuery("select * from testdata order by id");
  }

  @Test
  public void testEmpty() {
    RecordIterator it = source.getRecords();
    assertTrue(!it.hasNext());    
  }

  @Test
  public void testOneRow() {
    perform("insert into testdata values (1, 'foo')");

    source.addColumn(new Column("ID", null, null, null));
    source.addColumn(new Column("NAME", null, null, null));
        
    RecordIterator it = source.getRecords();
    assertTrue(it.hasNext());

    Record r = it.next();
    assertEquals("1", r.getValue("ID"));
    assertEquals("foo", r.getValue("NAME"));

    assertFalse(it.hasNext());
  }

  @Test
  public void testOneRowSkipColumn() {
    perform("insert into testdata values (1, 'foo')");

    source.addColumn(new Column("ID", null, null, null));
        
    RecordIterator it = source.getRecords();
    assertTrue(it.hasNext());

    Record r = it.next();
    assertEquals("1", r.getValue("ID"));
    assertEquals(null, r.getValue("NAME"));

    assertFalse(it.hasNext());
  }

  @Test
  public void testOneRowMapColumnTwice() {
    perform("insert into testdata values (1, 'smith, john')");

    RegexpCleaner givencleaner = new RegexpCleaner();
    givencleaner.setRegexp(", (.+)");
    RegexpCleaner familycleaner = new RegexpCleaner();
    familycleaner.setRegexp("^([^,]+), ");
    
    source.addColumn(new Column("ID", null, null, null));
    source.addColumn(new Column("NAME", "GIVENNAME", null, givencleaner));
    source.addColumn(new Column("NAME", "FAMILYNAME", null, familycleaner));
        
    RecordIterator it = source.getRecords();
    assertTrue(it.hasNext());

    Record r = it.next();
    assertEquals("1", r.getValue("ID"));
    assertEquals("john", r.getValue("GIVENNAME"));
    assertEquals("smith", r.getValue("FAMILYNAME"));

    assertFalse(it.hasNext());
  }

  @Test
  public void testNull() {
    perform("insert into testdata values (1, NULL)");

    LowerCaseNormalizeCleaner cleaner = new LowerCaseNormalizeCleaner();
    
    source.addColumn(new Column("ID", null, null, null));
    source.addColumn(new Column("NAME", "GIVENNAME", null, cleaner));
        
    RecordIterator it = source.getRecords();
    assertTrue(it.hasNext());

    Record r = it.next();
    assertEquals("1", r.getValue("ID"));
    assertEquals(null, r.getValue("GIVENNAME"));

    assertFalse(it.hasNext());
  }

  @Test
  public void testSplitting() {
    perform("insert into testdata values (1, 'foo bar baz')");

    source.addColumn(new Column("ID", null, null, null));
    Column col = new Column("NAME", null, null, null);
    col.setSplitOn(" ");
    source.addColumn(col);
        
    RecordIterator it = source.getRecords();
    assertTrue(it.hasNext());

    Record r = it.next();
    assertEquals("1", r.getValue("ID"));
    Collection<String> values = r.getValues("NAME");
    assertEquals(3, values.size());
    assertTrue(values.contains("foo"));
    assertTrue(values.contains("bar"));
    assertTrue(values.contains("baz"));

    assertFalse(it.hasNext());
  }
  
  // --- Helpers

  private void connect() {
    stmt = JDBCUtils.open(DRIVER, JDBC_URI, new Properties());
  }

  private void perform(String sql) {
    try {
      stmt.execute(sql);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }  
}
