
package no.priv.garshol.duke.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.comparators.QGramComparator;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class ObjectUtilsTest {
  private TestBean bean;
  private Map<String, Object> objects;

  @Before
  public void setup() {
    bean = new TestBean();
    objects = new HashMap();
  }
  
  @Test
  public void testOneWord() {
    ObjectUtils.setBeanProperty(bean, "property", "value", objects);
    assertEquals("property not set correctly", "value", bean.getProperty());
  }

  @Test
  public void testTwoWords() {
    ObjectUtils.setBeanProperty(bean, "property-name", "value", objects);
    assertEquals("property not set correctly", "value", bean.getPropertyName());
  }

  @Test
  public void testThreeWords() {
    ObjectUtils.setBeanProperty(bean, "long-property-name", "value", objects);
    assertEquals("property not set correctly", "value",
                 bean.getLongPropertyName());
  }

  @Test
  public void testIntProperty() {
    ObjectUtils.setBeanProperty(bean, "int-property", "25", objects);
    assertEquals("property not set correctly", 25,
                 bean.getIntProperty());
  }

  @Test
  public void testBoolProperty() {
    ObjectUtils.setBeanProperty(bean, "bool-property", "true", objects);
    assertEquals("property not set correctly", true,
                 bean.getBoolProperty());
  }

  @Test
  public void testDoubleProperty() {
    ObjectUtils.setBeanProperty(bean, "double-property", "0.25", objects);
    assertEquals("property not set correctly", 0.25,
                 bean.getDoubleProperty());
  }

  @Test
  public void testFloatProperty() {
    ObjectUtils.setBeanProperty(bean, "float-property", "0.25", objects);
    assertEquals("property not set correctly", 0.25f,
                 bean.getFloatProperty());
  }

  @Test
  public void testNamedObject() {
    objects.put("thetest", this);
    ObjectUtils.setBeanProperty(bean, "test", "thetest", objects);
    assertEquals("property not set correctly", this,
                 bean.getTest());
  }

  @Test
  public void testEnumConstant() {
    ObjectUtils.setBeanProperty(bean, "enum", "JACCARD", objects);
    assertEquals("property not set correctly", QGramComparator.Formula.JACCARD,
                 bean.getEnum());
  }

  @Test
  public void testCharProperty() {
    ObjectUtils.setBeanProperty(bean, "char-property", "g", objects);
    assertEquals("property not set correctly", 'g', bean.getCharProperty());
  }

  @Test
  public void testCharPropertyError() {
    try {
      ObjectUtils.setBeanProperty(bean, "char-property", "goo", objects);
      fail("shouldn't accept three-character string as value of character prop");
    } catch (DukeConfigException e) {
      // this is right. we're trying to set a three-character string
      // into a character property
    }
  }

  @Test
  public void testCollection() {
    objects.put("foo", "gurgle");
    objects.put("bar", "gargle");
    
    ObjectUtils.setBeanProperty(bean, "collection", "foo bar", objects);

    Collection<String> coll = bean.getCollection();
    assertEquals(2, coll.size());
    assertTrue(coll.contains("gurgle"));
    assertTrue(coll.contains("gargle"));
  }
  
  // ----- TESTBEAN

  public static class TestBean {
    private String value;
    private int theint;
    private boolean thebool;
    private double thedouble;
    private float thefloat;
    private char thechar;
    private ObjectUtilsTest thetest;
    private QGramComparator.Formula theenum;
    private Collection<String> collection;
    
    public void setProperty(String value) {
      this.value = value;
    }

    public String getProperty() {
      return value;
    }

    public void setPropertyName(String value) {
      this.value = value;
    }

    public String getPropertyName() {
      return value;
    }

    public void setLongPropertyName(String value) {
      this.value = value;
    }

    public String getLongPropertyName() {
      return value;
    }

    public void setIntProperty(int value) {
      this.theint = value;
    }

    public int getIntProperty() {
      return theint;
    }

    public void setBoolProperty(boolean value) {
      this.thebool = value;
    }

    public boolean getBoolProperty() {
      return thebool;
    }

    public void setDoubleProperty(double value) {
      this.thedouble = value;
    }

    public double getDoubleProperty() {
      return thedouble;
    }

    public void setFloatProperty(float value) {
      this.thefloat = value;
    }

    public float getFloatProperty() {
      return thefloat;
    }

    public void setTest(ObjectUtilsTest test) {
      this.thetest = test;
    }

    public ObjectUtilsTest getTest() {
      return thetest;
    }

    public void setCharProperty(char ch) {
      this.thechar = ch;
    }

    public char getCharProperty() {
      return thechar;
    }

    public void setEnum(QGramComparator.Formula theenum) {
      this.theenum = theenum;
    }

    public QGramComparator.Formula getEnum() {
      return theenum;
    }

    public Collection<String> getCollection() {
      return collection;
    }

    public void setCollection(Collection<String> collection) {
      this.collection = collection;
    }
  }
}