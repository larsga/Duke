
package no.priv.garshol.duke.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import no.priv.garshol.duke.CompactRecord;
import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.matchers.AbstractMatchListener;

import static org.junit.Assert.assertEquals;

public class TestUtils {

  public static Record makeRecord() {
    return new RecordImpl(new HashMap());    
  }

  public static Record makeRecord(String p1, String v1) {
    return makeRecord(p1, v1, null, null, null, null);
  }
  
  public static Record makeRecord(String p1, String v1, String p2, String v2) {
    return makeRecord(p1, v1, p2, v2, null, null);
  }

  public static Record makeRecord(String p1, String v1, String p2, String v2,
                            String p3, String v3) {
    CompactRecord rec = new CompactRecord();
    rec.addValue(p1, v1);
    if (v2 != null)
      rec.addValue(p2, v2);
    if (v3 != null)
      rec.addValue(p3, v3);
    return rec;
  }

  public static void verifySame(Link l1, Link l2) {
    assertEquals("wrong ID1", l1.getID1(), l2.getID1());
    assertEquals("wrong ID2", l1.getID2(), l2.getID2());
    assertEquals("wrong status", l1.getStatus(), l2.getStatus());
    assertEquals("wrong kind", l1.getKind(), l2.getKind());
    assertEquals(l1.getConfidence(), l2.getConfidence(), 0.0001);
  }

  public static class TestListener extends AbstractMatchListener {
    private List<Pair> matches;
    private int records;
    private int nomatch;
    private int maybes;

    public TestListener() {
      this.matches = new ArrayList();
    }
    
    public List<Pair> getMatches() {
      return matches;
    }

    public int getRecordCount() {
      return records;
    }

    public int getNoMatchCount() {
      return nomatch;
    }

    public int getMaybeCount() {
      return maybes;
    }

    public void batchReady(int size) {
      records += size;
    }
    
    public void matches(Record r1, Record r2, double confidence) {
      matches.add(new Pair(r1, r2, confidence));
    }

    public void matchesPerhaps(Record r1, Record r2, double confidence) {
      maybes++;
    }
    
    public void noMatchFor(Record r) {
      nomatch++;
    }
  }
  
  public static class Pair {
    public Record r1;
    public Record r2;
    public double conf;

    public Pair(Record r1, Record r2, double conf) {
      this.r1 = r1;
      this.r2 = r2;
      this.conf = conf;
    }
  }
}