package no.priv.garshol.duke.test;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.DataSource;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.datasources.CSVDataSource;
import no.priv.garshol.duke.datasources.Column;
import no.priv.garshol.duke.transforms.TransformDataSource;
import no.priv.garshol.duke.transforms.TransformOperation;
import no.priv.garshol.duke.transforms.TransformOperationJoin;

import org.junit.Assert;
import org.junit.Test;

public class TransformDataSourceTest {

	@Test
	public void testTransformation() {

		CSVDataSource source = new CSVDataSource();
		source.addColumn(new Column("NAME", null, null, null));
		source.addColumn(new Column("ADDR1", null, null, null));
		source.addColumn(new Column("ADDR2", null, null, null));
		source.addColumn(new Column("CITY", null, null, null));

		String csvdata = "NAME,ADDR1,ADDR2,CITY\nJohn Smith,10 rue des Iris,Bat.4,Paris\nMartin Dupont,7 av Jean Moulin,,Lyon";
		source.setReader(new StringReader(csvdata));

		TransformOperationJoin join = new TransformOperationJoin();
		join.setProperties("ADDR1 ADDR2");
		join.setResultingProperty("ADDRESS");
		join.setJoiner(" ");

		TransformDataSource tds = new TransformDataSource(source);
		tds.addOperation(join);

		RecordIterator iter = tds.getRecords();

		Record john = iter.next();
		assertTrue(john.getProperties().contains("ADDRESS"));
		Assert.assertEquals("10 rue des Iris Bat.4", john.getValue("ADDRESS"));

		Record martin = iter.next();
		Assert.assertEquals("7 av Jean Moulin", martin.getValue("ADDRESS"));

		Record end = iter.next();
		Assert.assertNull(end);
	}

	@Test
	public void testConfiguration() throws Exception {
		Configuration config = ConfigLoader.load("classpath:config-transform.xml");
		
		DataSource ds = (DataSource) config.getDataSources().toArray()[0];
		Assert.assertNotNull(ds);
		Assert.assertTrue(ds instanceof TransformDataSource);
		
		TransformDataSource tds = (TransformDataSource) ds;
		Assert.assertEquals(1, tds.getOperations().size());
		
		TransformOperation operation = tds.getOperations().get(0);
		Assert.assertTrue(operation instanceof TransformOperationJoin);
	}

}
