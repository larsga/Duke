
package no.priv.garshol.duke.test;

import no.priv.garshol.duke.InMemoryClassDatabase;
import no.priv.garshol.duke.EquivalenceClassDatabase;

public class InMemoryClassDatabaseTest extends ClassDatabaseTest {

  public EquivalenceClassDatabase createDatabase() {
    return new InMemoryClassDatabase();
  }
  
}