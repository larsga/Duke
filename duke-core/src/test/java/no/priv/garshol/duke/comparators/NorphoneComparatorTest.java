
package no.priv.garshol.duke.comparators;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

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

  @Test
  public void testSanderhaugen() {
    same("Sannerhaugen", "Sanderhaugen");
  }

  @Test
  public void testJahren() {
    same("Jahren", "Jaren");
  }

  @Test
  public void testAmundsroed() {
    same("Amundsrud", "Amundsr\u00F8d");
  }

  @Test
  public void testCarlson() {
    same("Karlson", "Carlson");
  }
  
  private void same(String key1, String key2) {
    assertEquals("wrong key '" + key1 + "' != '" + key2 + "'",
                 NorphoneComparator.norphone(key1),
                 NorphoneComparator.norphone(key2));
  }
  
  private void different(String str1, String str2) {
    String key1 = NorphoneComparator.norphone(str1);
    String key2 = NorphoneComparator.norphone(str2);

    if (key1.equals(key2))
      fail("'" + str1 + "' and '" + str2 + "' produce same key: " + key1);
  }
}