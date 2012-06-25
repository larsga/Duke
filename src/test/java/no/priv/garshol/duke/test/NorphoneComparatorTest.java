
package no.priv.garshol.duke.test;

import org.junit.Test;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.comparators.NorphoneComparator;

public class NorphoneComparatorTest {

  @Test
  public void testEmpty() {
    same("", "");
  }

  @Test
  public void testAarrestad() {
    same("Aarrestad", "\u00C5rrestad");
  }

  @Test
  public void testAndreassen() {
    same("Andreasen", "Andreassen");
  }

  @Test
  public void testArntsen() {
    same("Arntsen", "Arntzen");
  }

  @Test
  public void testBache() {
    same("Bache", "Bakke");
  }

  @Test
  public void testFranck() {
    same("Frank", "Franck");
  }

  @Test
  public void testChristian() {
    same("Christian", "Kristian");
  }

  @Test
  public void testKielland() {
    same("Kielland", "Kjelland");
  }

  @Test
  public void testKrogh() {
    same("Krogh", "Krog");
  }

  @Test
  public void testKrohg() {
    same("Krog", "Krohg");
  }
  
  @Test
  public void testJendahl() {
    same("Jendal", "Jendahl");
  }

  @Test
  public void testHjendal() {
    same("Jendal", "Hjendal");
  }

  @Test
  public void testGjendal() {
    same("Jendal", "Gjendal");
  }

  @Test
  public void testWold() {
    same("Vold", "Wold");
  }

  @Test
  public void testThomas() {
    same("Thomas", "Tomas");
  }

  @Test
  public void testAamodt() {
    same("Aamodt", "Aamot");
  }

  @Test
  public void testAksel() {
    same("Aksel", "Axel");
  }

  @Test
  public void testChristophersen() {
    same("Kristoffersen", "Christophersen");
  }

  @Test
  public void testVold() {
    same("Voll", "Vold");
  }

  @Test
  public void testGranlid() {
    same("Granli", "Granlid");
  }

  @Test
  public void testGiever() {
    same("Gjever", "Giever");
  }
  
  private void same(String key1, String key2) {
    assertEquals("wrong key '" + key1 + "' != '" + key2 + "'",
                 NorphoneComparator.norphone(key1),
                 NorphoneComparator.norphone(key2));
  }
}