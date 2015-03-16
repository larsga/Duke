
package no.priv.garshol.duke.matchers;

import no.priv.garshol.duke.InMemoryLinkDatabase;
import no.priv.garshol.duke.LinkDatabase;

public class InMemoryLinkDatabaseMatchListenerTest
  extends LinkDatabaseMatchListenerTest {

  protected LinkDatabase makeDatabase() {
    return new InMemoryLinkDatabase();
  }

}