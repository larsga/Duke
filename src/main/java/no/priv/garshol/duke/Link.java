
package no.priv.garshol.duke;

import java.util.Collection;

public class Link {

  public String getID1() {
    return null;
  }
  
  public String getID2() {
    return null;
  }

  public long getTimestamp() {
    return 0;
  }

  public void getStatus() {
    // FIXME: create an enum for this
    // values:
    //   asserted: outside evidence tells us this is true
    //   inferred: we believe this is true
    //   deleted:  we used to think it was true, but don't any more
    //   denied:   outside evidence tells us this is false
  }

  public void getKind() {
    // FIXME: enum?
    // values:
    //   certain:    produce owl:sameAs (inferred from status == true)
    //   uncertain:  if confidence was too low
    //   denied:     produce owl:differentIndividual (inferred from 'denied'?)
  }
  
}