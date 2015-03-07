
package no.priv.garshol.duke.test;

import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.InMemoryLinkDatabase;

public class InMemoryLinkDatabaseMatchListenerTest
  extends LinkDatabaseMatchListenerTest {

  protected LinkDatabase makeDatabase() {
    return new InMemoryLinkDatabase();
  }

}