
package no.priv.garshol.duke.comparators;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class MetaphoneComparatorTest {

  @Test
  public void testEmpty() {
    check("", "");
  }

  @Test
  public void testIndia() {
    check("INT", "India");
  }

  @Test
  public void testSmith() {
    check("SM0", "Smith");
  }

  @Test
  public void testHowl() {
    check("HL", "Howl");
  }

  @Test
  public void testTesting() {
    check("TSTNK", "testing");
  }

  @Test
  public void testTesting2() {
    check("TSTNK", "TESTING");
  }

  @Test
  public void testThe() {
    check("0", "The");
  }

  @Test
  public void testThe2() {
    check("0", "the");
  }

  @Test
  public void testBrown() {
    check("BRN", "Brown");
  }

  @Test
  public void testFox() {
    check("FKS", "Fox");
  }

  @Test
  public void testJumped() {
    check("JMPT", "Jumped");
  }

  @Test
  public void testOver() {
    check("OFR", "Over");
  }

  @Test
  public void testLazy() {
    check("LS", "Lazy");
  }

  @Test
  public void testDogs() {
    check("TKS", "dOGS");
  }

  @Test
  public void testComb() {
    check("KM", "Comb");
  }

  @Test
  public void testTomb() {
    check("TM", "Tomb");
  }

  @Test
  public void testWomb() {
    check("WM", "Womb");
  }

  @Test
  public void testWhy() {
    check("W", "Why");
  }

  @Test
  public void testCiapo() {
    check("XP", "Ciapo");
  }

  @Test
  public void testI() {
    check("I", "I");
  }

  @Test
  public void testIkk() {
    check("IK", "IKK");
  }

  @Test
  public void testIk() {
    check("IK", "IK");
  }

  @Test
  public void testEek() {
    check("EK", "eek");
  }

  @Test
  public void testIkkikkikk() {
    check("IKKK", "Ikkikkikk");
  }

  @Test
  public void testHicc() {
    check("HKK", "Hicc");
  }

  @Test
  public void testKnife() {
    check("NF", "Knife");
  }

  @Test
  public void testCesar() {
    check("SSR", "cesar");
  }

  @Test
  public void testChe() {
    check("X", "che");
  }

  @Test
  public void testCIA() {
    check("X", "CIA");
  }

  @Test
  public void testSchia() {
    check("SK", "Schia");
  }

  @Test
  public void testCa() {
    check("K", "Ca");
  }

  @Test
  public void testDodgy() {
    check("TJ", "Dodgy");
  }

  @Test
  public void testDoggy() {
    check("TK", "Doggy");
  }

  @Test
  public void testGi() {
    check("J", "Gi");
  }

  @Test
  public void testDig() {
    check("TK", "Dig");
  }

  @Test
  public void testDoge() {
    check("TJ", "Doge");
  }

  @Test
  public void testDoughy() {
    check("T", "Doughy");
  }

  @Test
  public void testRough() {
    check("R", "Rough"); 
  }

  @Test
  public void testGherkin() {
    // FIXME: algorithm seems to say answer should be "RKN", but
    // Brogden's code produces "KRKN".  that seems better, but not in
    // line with algorithm.
    check("RKN", "Gherkin");
  }

  @Test
  public void testAgnes() {
    check("ANS", "Agnes"); 
  }

  @Test
  public void testAha() {
    check("AH", "Aha"); 
  }

  @Test
  public void testKahn() {
    check("KN", "Kahn"); 
  }

  @Test
  public void testB() {
    check("B", "B");
  }

  @Test
  public void testPackage() {
    check("PKJ", "Package");
  }

  @Test
  public void testRalph() {
    check("RLF", "Ralph");
  }

  @Test
  public void testQuintin() {
    check("KNTN", "Quintin");
  }

  @Test
  public void testShit() {
    check("XT", "Shit");
  }

  @Test
  public void testConversion() {
    check("KNFRXN", "Conversion");
  }

  @Test
  public void testMartian() {
    check("MRXN", "Martian");
  }

  @Test
  public void testAitch() {
    check("AX", "Aitch");
  }

  @Test
  public void testXnxnx() {
    check("SNKSNKS", "Xnxnx");
  }

  @Test
  public void testOhyes() {
    check("OHYS", "ohyes");
  }

  @Test
  public void testXavier() {
    check("SFR", "Xavier");
  }

  @Test
  public void testAerated() {
    check("ERTT", "Aerated");
  }

  @Test
  public void testWrite() {
    check("RT", "Write");
  }

  @Test
  public void testSverdrup() {
    // testing this because it used to crash the comparator
    check("SFRTRP", "sverdrup");
  }
  
  private void check(String key, String value) {
    assertEquals("wrong key for '" + value + "'",
                 key, MetaphoneComparator.metaphone(value));
  }
}