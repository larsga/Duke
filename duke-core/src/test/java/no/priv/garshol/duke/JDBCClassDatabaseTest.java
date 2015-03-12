
package no.priv.garshol.duke;

import java.util.Properties;

import no.priv.garshol.duke.test.ClassDatabaseTest;

public class JDBCClassDatabaseTest extends ClassDatabaseTest {

  public EquivalenceClassDatabase createDatabase() {
    return new JDBCEquivalenceClassDatabase("org.h2.Driver", "jdbc:h2:mem:",
                                            "h2", new Properties());
  }
  
}