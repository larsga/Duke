
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
    ObjectUtils.setBeanProperty(bean, "int-property", "25");
    assertEquals("property not set correctly", 25,
                 bean.getIntProperty());
  }

  @Test
  public void testBoolProperty() {
    bean = new TestBean();
    ObjectUtils.setBeanProperty(bean, "bool-property", "true");
    assertEquals("property not set correctly", true,
                 bean.getBoolProperty());
  }

  @Test
  public void testDoubleProperty() {
    bean = new TestBean();
    ObjectUtils.setBeanProperty(bean, "double-property", "0.25");
    assertEquals("property not set correctly", 0.25,
                 bean.getDoubleProperty());
  }
  
  // ----- TESTBEAN

  public static class TestBean {
    private String value;
    private int theint;
    private boolean thebool;
    private double thedouble;

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
  }
}