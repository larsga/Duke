
package no.priv.garshol.duke.test;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.ObjectUtils;

public class ObjectUtilsTest {
  private TestBean bean;
  
  @Test
  public void testOneWord() {
    bean = new TestBean();
    ObjectUtils.setBeanProperty(bean, "property", "value");
    assertEquals("property not set correctly", "value", bean.getProperty());
  }

  @Test
  public void testTwoWords() {
    bean = new TestBean();
    ObjectUtils.setBeanProperty(bean, "property-name", "value");
    assertEquals("property not set correctly", "value", bean.getPropertyName());
  }

  @Test
  public void testThreeWords() {
    bean = new TestBean();
    ObjectUtils.setBeanProperty(bean, "long-property-name", "value");
    assertEquals("property not set correctly", "value",
                 bean.getLongPropertyName());
  }

  @Test
  public void testIntProperty() {
    bean = new TestBean();
    ObjectUtils.setBeanProperty(bean, "int-property", 25);
    assertEquals("property not set correctly", 25,
                 bean.getIntProperty());
  }
  
  // ----- TESTBEAN

  public static class TestBean {
    private String value;
    private int theint;

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
  }
}