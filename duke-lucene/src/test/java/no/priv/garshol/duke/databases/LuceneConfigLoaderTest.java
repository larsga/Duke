
package no.priv.garshol.duke.databases;

import java.io.IOException;

import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Database;
import org.junit.Test;
import org.xml.sax.SAXException;

import static junit.framework.Assert.assertEquals;

public class LuceneConfigLoaderTest {

  @Test
  public void testDatabase() throws IOException, SAXException {
    Configuration config = ConfigLoader.load("classpath:config-database.xml");
    Database db = config.getDatabase(false);
    LuceneDatabase lucene = (LuceneDatabase) db;
    assertEquals("/tmp/ct-visma-1", lucene.getPath());
  }

}
