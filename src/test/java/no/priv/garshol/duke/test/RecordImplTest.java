
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import junit.framework.AssertionFailedError;

import java.util.Collection;
import java.util.Collections;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;

public class RecordImplTest {

  @Test
  public void testNormal() {
    Record r = TestUtils.makeRecord("ID", "abc", "NAME", "b");

    assertEquals("abc", r.getValue("ID"));
    Collection<String> values = r.getValues("ID");
    assertEquals(1, values.size());
    assertEquals("abc", values.iterator().next());
    
    assertEquals("b", r.getValue("NAME"));
    values = r.getValues("NAME");
    assertEquals(1, values.size());
    assertEquals("b", values.iterator().next());

    assertEquals(null, r.getValue("EMAIL"));
    assertTrue(r.getValues("EMAIL").isEmpty());
  }
  
}