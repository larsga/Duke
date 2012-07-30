
package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.cleaners.NorwegianAddressCleaner;

public class NorwegianAddressCleanerTest {
  private NorwegianAddressCleaner cleaner;
  
  @Before
  public void setup() {
    cleaner = new NorwegianAddressCleaner();
  }
  
  @Test
  public void testEmpty() {
    test("", "");
  }

  @Test
  public void testVeienVeien() {
    test("grefsenveien 132", "grefsenveien 132");
  }
  
  @Test
  public void testVeienVnDot() {
    test("grefsenvn. 132", "grefsenveien 132");
  }

  @Test
  public void testVnVnDot() {
    test("grefsenvn. 132", "grefsenvn 132");
  }

  @Test
  public void testVeienVn() {
    test("grefsenvn. 132", "grefsenveien 132");
  }

  @Test
  public void testGtGata() {
    test("kanalgt 3", "kanalgata 3");
  }

  @Test
  public void testGtDotGata() {
    test("kanalgt. 3", "kanalgata 3");
  }

  @Test
  public void testGataGata() {
    test("kanalgata 3", "kanalgata 3");
  }

  @Test
  public void testGtDotGaten() {
    test("enerhauggt. 5", "enerhauggaten 5");
  }

  @Test
  public void testBoksPostboks() {
    test("boks 367", "postboks 367");
  }

  @Test
  public void testBoksPostboksComma() {
    test("boks 1353 vika", "postboks 1353,vika");
  }

  @Test
  public void testPostboksPb() {
    test("postboks 2623 møhlenpris", "pb 2623 møhlenpris");
  }

  @Test
  public void testPostboksPbDot() {
    test("postboks 6258 etterstad", "pb. 6258 etterstad");
  }

  @Test
  public void testBoksCommaBoks() {
    test("boks 24, grefsen", "boks 24 grefsen");
  }

  @Test
  public void testPostboksDashPostboks() {
    test("postboks 124 - bryn", "postboks 124 bryn");
  }

  @Test
  public void testVeienVndotnospace() {
    test("Okernveien 38 b", "Okernvn.38 b");
  }

  @Test
  public void testNumberletterNumberspaceletter() {
    test("ammerudveien 31d", "ammerudveien 31 d");
  }

  @Test
  public void testSpaceVSpaceDigit() {
    test("cecilie thoresens v 5", "cecilie thoresens vei 5");
  }

  @Test
  public void testSpaceVDotSpaceDigit() {
    test("cecilie thoresens v. 5", "cecilie thoresens vei 5");
  }

  @Test
  public void testVDotSpaceDigit() {
    test("varnav. 32a", "varnaveien 32a");
  }
  
  private void test(String s1, String s2) {
    assertEquals(cleaner.clean(s1), cleaner.clean(s2));
  }
  
}