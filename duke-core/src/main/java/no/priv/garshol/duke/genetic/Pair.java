
package no.priv.garshol.duke.genetic;

/**
 * Represents a pair of records.
 */
public class Pair {
  public String id1;
  public String id2;
  public int counter;
  public boolean[] believers; // which configurations think this pair is correct

  public Pair(String id1, String id2) {
    this.id1 = id1;
    this.id2 = id2;
  }

  public boolean equals(Object other) {
    if (!(other instanceof Pair))
      return false;

    Pair opair = (Pair) other;
    return opair.id1.equals(id1) && opair.id2.equals(id2);
  }

  public int hashCode() {
    return id1.hashCode() + id2.hashCode();
  }
}
