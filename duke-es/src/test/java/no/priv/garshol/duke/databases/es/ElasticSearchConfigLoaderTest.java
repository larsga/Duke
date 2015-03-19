package no.priv.garshol.duke.databases.es;

import java.io.IOException;

import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.databases.es.ElasticSearchDatabase;

import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;

public class ElasticSearchConfigLoaderTest {

	@Test
	public void testDatabase() throws IOException, SAXException {
		Configuration config = ConfigLoader
				.load("classpath:config-database.xml");
		Database db = config.getDatabase(false);
		ElasticSearchDatabase es = (ElasticSearchDatabase) db;
		assertEquals("duke-es", es.getCluster());
	}

}
