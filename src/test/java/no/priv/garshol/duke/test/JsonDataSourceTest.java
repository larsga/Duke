package no.priv.garshol.duke.test;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.datasources.Column;
import no.priv.garshol.duke.datasources.JsonDataSource;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by damien on 08/04/14.
 */
public class JsonDataSourceTest {
    private JsonDataSource source;

    @Before
    public void setup() {
        source = new JsonDataSource();
        source.addColumn(new Column("F1", null, null, null));
        source.addColumn(new Column("F2", null, null, null));
        source.addColumn(new Column("F3", null, null, null));
    }


    @Test
    public void testEmpty() throws IOException {
        RecordIterator it = source.getRecordsFromString("");
        assertTrue(!it.hasNext());
    }


   @Test
    public void testSingleRecord() throws IOException {
        Record r = source.getRecordsFromString("{\"F1\":\"a\",\"F2\" : \"b\", \"F3\" : \"c\", \"F4\" : \"d\"}").next();

        assertEquals("a", r.getValue("F1"));
        assertEquals("b", r.getValue("F2"));
        assertEquals("c", r.getValue("F3"));
    }

    @Test
    public void testArrayField() {
        Record r = source.getRecordsFromString("{\"F1\":[\"a\",\"b\",\"c\"]}").next();
        for (String p : r.getProperties()){
            System.out.println("prop : " + p);
        }
        assertEquals(3,r.getValues("F1").size());
    }

    @Test
    public void testNestRecords() {
        Record r = source.getRecordsFromString("{\"F1\":\"a\",\"FF2\" : {\"F2\" : \"b\"}, \"FFF3\" : {\"FF3\" : {\"F3\" : \"c\",\"F4\" : \"d\"}}}").next();
        assertEquals("a", r.getValue("F1"));
        assertEquals("b", r.getValue("F2"));
        assertEquals("c", r.getValue("F3"));
    }
   @Test
    public void multipleRecords() {
        RecordIterator it = source.getRecordsFromString("{\"F1\":\"a\",\"F2\" : \"b\", \"F3\" : \"c\"}{\"F1\":\"a2\",\"F2\" : \"b2\", \"F3\" : \"c2\"}");
        Record r1 = it.next();
        assertEquals("a", r1.getValue("F1"));
        assertEquals("b", r1.getValue("F2"));
        assertEquals("c", r1.getValue("F3"));
        Record r2 =  it.next();
        assertEquals("a2", r2.getValue("F1"));
        assertEquals("b2", r2.getValue("F2"));
        assertEquals("c2", r2.getValue("F3"));
    }
/*
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
*/
}

