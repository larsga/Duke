
package no.priv.garshol.duke;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class CompactRecordTest {

  @Test
  public void testEmpty() {
    CompactRecord r = new CompactRecord();
    r.toString();

    assertTrue(r.isEmpty());
    assertTrue(r.getProperties().isEmpty());
    assertTrue(r.getValues("foo").isEmpty());
    assertTrue(r.getValue("foo") == null);
  }

  @Test
  public void testSingle() {
    CompactRecord r = new CompactRecord();
    r.addValue("foo", "bar");
    r.toString();

    assertTrue(!r.isEmpty());
    assertTrue(r.getProperties().size() == 1);
    assertTrue(r.getProperties().iterator().next().equals("foo"));
    assertTrue(r.getValues("foo").iterator().next().equals("bar"));
    assertTrue(r.getValue("foo").equals("bar"));
  }
}
