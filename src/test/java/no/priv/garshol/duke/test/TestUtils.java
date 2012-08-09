
package no.priv.garshol.duke.test;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.matchers.AbstractMatchListener;

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
    HashMap props = new HashMap();
    props.put(p1, Collections.singleton(v1));
    if (v2 != null)
      props.put(p2, Collections.singleton(v2));
    if (v3 != null)
      props.put(p3, Collections.singleton(v3));
    return new RecordImpl(props);
  }
  
  static class TestListener extends AbstractMatchListener {
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

    public void startRecord(Record r) {
      records++;
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
  
  static class Pair {
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