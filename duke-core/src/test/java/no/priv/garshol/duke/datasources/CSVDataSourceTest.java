
package no.priv.garshol.duke.datasources;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;

import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.cleaners.LowerCaseNormalizeCleaner;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CSVDataSourceTest {
  private CSVDataSource source;

  @Before
  public void setup() {
    source = new CSVDataSource();
  }

  @Test
  public void testEmpty() throws IOException {
    RecordIterator it = read("");
    assertTrue(!it.hasNext());
  }

  @Test
  public void testSingleRecord() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", null, null, null));
    source.addColumn(new Column("F3", null, null, null));

    RecordIterator it = read("F1,F2,F3\na,b,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }

  @Test
  public void testSingleRecordWithComment() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", null, null, null));
    source.addColumn(new Column("F3", null, null, null));
    source.setSkipLines(1);

    RecordIterator it = read("# this is a comment\nF1,F2,F3\na,b,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }

  @Test
  public void testSingleRecordWithoutHeader() throws IOException {
    source.addColumn(new Column("1", "F1", null, null));
    source.addColumn(new Column("2", "F2", null, null));
    source.addColumn(new Column("3", "F3", null, null));
    source.setHeaderLine(false);

    RecordIterator it = read("a,b,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }

  @Test
  public void testSingleRecordWithoutHeaderExtraColumn() throws IOException {
    source.addColumn(new Column("1", "F1", null, null));
    source.addColumn(new Column("2", "F2", null, null));
    source.addColumn(new Column("3", "F3", null, null));
    source.setHeaderLine(false);

    RecordIterator it = read("a,b,c,d");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }

  @Test
  public void testSingleRecordWithoutHeaderSkipColumn() throws IOException {
    source.addColumn(new Column("1", "F1", null, null));
    source.addColumn(new Column("3", "F3", null, null));
    source.setHeaderLine(false);

    RecordIterator it = read("a,b,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals(null, r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }

  @Test
  public void testColumnNotInHeader() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", null, null, null));
    source.addColumn(new Column("F3", null, null, null));
    source.addColumn(new Column("F4", null, null, null));

    try {
      RecordIterator it = read("F1,F2,F3\na,b,c");
      Record r = it.next();
      fail("Didn't catch missing column F4");
    } catch (DukeConfigException e) {
      // caught the configuration mistake
    }
  }

  @Test
  public void testHeaderNotInConfig() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", null, null, null));
    source.addColumn(new Column("F3", null, null, null));
    source.addColumn(new Column("F4", null, null, null));

    try {
      RecordIterator it = read("F5,F2,F3\na,b,c");
      Record r = it.next();
      fail("Didn't catch unknown column F5");
    } catch (DukeConfigException e) {
      // caught the configuration mistake
    }
  }

  @Test
  public void testSplitting() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    Column c = new Column("F2", null, null, null);
    c.setSplitOn(";");
    source.addColumn(c);
    source.addColumn(new Column("F3", null, null, null));

    RecordIterator it = read("F1,F2,F3\na,b;d;e,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("c", r.getValue("F3"));

    Collection<String> values = r.getValues("F2");
    assertEquals(3, values.size());
    assertTrue(values.contains("b"));
    assertTrue(values.contains("d"));
    assertTrue(values.contains("e"));
  }

  @Test
  public void testSplittingCleaning() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    Column c = new Column("F2", null, null, new LowerCaseNormalizeCleaner());
    c.setSplitOn(";");
    source.addColumn(c);
    source.addColumn(new Column("F3", null, null, null));

    RecordIterator it = read("F1,F2,F3\na, b ; d ; e ,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("c", r.getValue("F3"));

    Collection<String> values = r.getValues("F2");
    assertEquals(3, values.size());
    assertTrue(values.contains("b"));
    assertTrue(values.contains("d"));
    assertTrue(values.contains("e"));
  }

  @Test
  public void testNoValueForEmpty() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", null, null, null));
    source.addColumn(new Column("F3", null, null, null));

    RecordIterator it = read("F1,F2,F3\na,b,");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals(r.getValue("F3"), null);
  }

  @Test
  public void testNoValueForEmptySplit() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    Column c = new Column("F2", null, null, null);
    c.setSplitOn(";");
    source.addColumn(c);
    source.addColumn(new Column("F3", null, null, null));

    RecordIterator it = read("F1,F2,F3\na,b;;e,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("c", r.getValue("F3"));

    Collection<String> values = r.getValues("F2");
    assertEquals(2, values.size());
    assertTrue(values.contains("b"));
    assertTrue(values.contains("e"));
  }

  @Test
  public void testSeparator() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", null, null, null));
    source.addColumn(new Column("F3", null, null, null));

    RecordIterator it = read("F1;F2;F3\na;b;c", ';');

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2"));
    assertEquals("c", r.getValue("F3"));
  }

  @Test
  public void testMissingHeader() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", null, null, null));
    source.addColumn(new Column("F3", null, null, null));

    try {
      RecordIterator it = read("", ';');
      fail("accepted file with no header");
    } catch (DukeException e) {
      // as wanted
    }
  }

  @Test
  public void testUseColumnTwice() throws IOException {
    source.addColumn(new Column("F1", null, null, null));
    source.addColumn(new Column("F2", "F2a", null,
                                new LowerCaseNormalizeCleaner()));
    source.addColumn(new Column("F2", "F2b", null, null));
    source.addColumn(new Column("F3", null, null, null));

    RecordIterator it = read("F1,F2,F3\na,B,c");

    Record r = it.next();
    assertEquals("a", r.getValue("F1"));
    assertEquals("b", r.getValue("F2a"));
    assertEquals("B", r.getValue("F2b"));
    assertEquals("c", r.getValue("F3"));
  }

  // ===== UTILITIES

  private RecordIterator read(String csvdata) {
    return read(csvdata, ',');
  }

  private RecordIterator read(String csvdata, char separator) {
    source.setReader(new StringReader(csvdata));
    source.setSeparator(separator);
    return source.getRecords();
  }
}
