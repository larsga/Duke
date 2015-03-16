
package no.priv.garshol.duke.test;

import java.util.Collection;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.EquivalenceClassDatabase;

public abstract class ClassDatabaseTest {
  private EquivalenceClassDatabase classdb;
  
  @Before
  public void setup() {
    classdb = createDatabase();
  }

  public abstract EquivalenceClassDatabase createDatabase();
  
  @Test
  public void testEmpty() {
    assertTrue("nothing's happened, so there should be no links",
               classdb.getClass("id1").isEmpty());
  }

  @Test
  public void testSingle() {
    classdb.addLink("id1", "id2");
    classdb.commit();

    assertTrue("unknown ID should have no class",
               classdb.getClass("id3").isEmpty());

    checkClass(classdb.getClass("id1"), "id1", "id2");
    checkClass(classdb.getClass("id2"), "id1", "id2");
  }

  @Test
  public void testSingleSymmetric() {
    classdb.addLink("id1", "id2");
    classdb.addLink("id2", "id1");
    classdb.commit();

    assertTrue("unknown ID should have no class",
               classdb.getClass("id3").isEmpty());

    checkClass(classdb.getClass("id1"), "id1", "id2");
    checkClass(classdb.getClass("id2"), "id1", "id2");
  }

  @Test
  public void testDouble() {
    classdb.addLink("id1", "id2");
    classdb.addLink("id3", "id4");
    classdb.commit();

    assertTrue("unknown ID should have no class",
               classdb.getClass("id5").isEmpty());

    checkClass(classdb.getClass("id1"), "id1", "id2");
    checkClass(classdb.getClass("id2"), "id1", "id2");
    checkClass(classdb.getClass("id3"), "id3", "id4");
    checkClass(classdb.getClass("id4"), "id3", "id4");
  }

  @Test
  public void testDoubleMerge() {
    classdb.addLink("id1", "id2");
    classdb.addLink("id3", "id4");
    classdb.addLink("id1", "id3");
    classdb.commit();

    assertTrue("unknown ID should have no class",
               classdb.getClass("id5").isEmpty());

    checkClass(classdb.getClass("id1"), "id1", "id2", "id3", "id4");
    checkClass(classdb.getClass("id2"), "id1", "id2", "id3", "id4");
    checkClass(classdb.getClass("id3"), "id1", "id2", "id3", "id4");
    checkClass(classdb.getClass("id4"), "id1", "id2", "id3", "id4");
  }

  @Test
  public void testFourOneByOne() {
    classdb.addLink("id1", "id2");
    classdb.addLink("id1", "id3");
    classdb.addLink("id1", "id4");
    classdb.commit();

    assertTrue("unknown ID should have no class",
               classdb.getClass("id5").isEmpty());

    checkClass(classdb.getClass("id1"), "id1", "id2", "id3", "id4");
    checkClass(classdb.getClass("id2"), "id1", "id2", "id3", "id4");
    checkClass(classdb.getClass("id3"), "id1", "id2", "id3", "id4");
    checkClass(classdb.getClass("id4"), "id1", "id2", "id3", "id4");
  }

  @Test
  public void testFourOneByOne2() {
    classdb.addLink("id2", "id1");
    classdb.addLink("id1", "id3");
    classdb.addLink("id4", "id1");
    classdb.commit();

    assertTrue("unknown ID should have no class",
               classdb.getClass("id5").isEmpty());

    checkClass(classdb.getClass("id1"), "id1", "id2", "id3", "id4");
    checkClass(classdb.getClass("id2"), "id1", "id2", "id3", "id4");
    checkClass(classdb.getClass("id3"), "id1", "id2", "id3", "id4");
    checkClass(classdb.getClass("id4"), "id1", "id2", "id3", "id4");
  }

  @Test
  public void testBiggerIntoSmaller() {
    classdb.addLink("id1", "id2");
    classdb.addLink("id3", "id4");
    classdb.addLink("id5", "id6");
    classdb.addLink("id4", "id5");
    classdb.addLink("id1", "id3");
    classdb.commit();

    assertTrue("unknown ID should have no class",
               classdb.getClass("id7").isEmpty());

    checkClass(classdb.getClass("id1"), "id1", "id2", "id3", "id4", "id5", "id6");
    checkClass(classdb.getClass("id2"), "id1", "id2", "id3", "id4", "id5", "id6");
    checkClass(classdb.getClass("id3"), "id1", "id2", "id3", "id4", "id5", "id6");
    checkClass(classdb.getClass("id4"), "id1", "id2", "id3", "id4", "id5", "id6");
    checkClass(classdb.getClass("id5"), "id1", "id2", "id3", "id4", "id5", "id6");
    checkClass(classdb.getClass("id6"), "id1", "id2", "id3", "id4", "id5", "id6");
  }
  
  private void checkClass(Collection<String> klass, String... ids) {
    assertEquals("wrong size of class", klass.size(), ids.length);
    for (String id : ids)
      assertTrue("class must contain " + id, klass.contains(id));
  }
}
