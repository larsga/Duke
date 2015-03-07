
package no.priv.garshol.duke.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.ConfigWriter;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.comparators.Levenshtein;
import no.priv.garshol.duke.datasources.CSVDataSource;
import no.priv.garshol.duke.datasources.Column;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
  
public class ConfigWriterTest {
  @Rule
  public TemporaryFolder tmpdir = new TemporaryFolder();

  @Test
  public void testEmpty() throws IOException, SAXException {
    Configuration config = ConfigLoader.load("classpath:config-empty.xml");

    assertTrue(config.getDataSources().isEmpty());
    assertTrue(config.getDataSources(1).isEmpty());
    assertTrue(config.getDataSources(2).isEmpty());
    assertEquals(config.getThreshold(), 0.4);
    assertEquals(config.getMaybeThreshold(), 0.0);
    assertTrue(config.getProperties().isEmpty());

    File outfile = tmpdir.newFile("config.xml");
    ConfigWriter writer = new ConfigWriter(new FileOutputStream(outfile.getAbsolutePath()));
    writer.write(config);
    config = ConfigLoader.load(outfile.getAbsolutePath());
    
    assertTrue(config.getDataSources().isEmpty());
    assertTrue(config.getDataSources(1).isEmpty());
    assertTrue(config.getDataSources(2).isEmpty());
    assertEquals(config.getThreshold(), 0.4);
    assertEquals(config.getMaybeThreshold(), 0.0);
    assertTrue(config.getProperties().isEmpty());
  }

  @Test
  public void testDefaultProbs() throws IOException, SAXException {
    // --- build config
    Levenshtein lev = new Levenshtein();
    
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    props.add(new PropertyImpl("NAME", lev, 0.3, 0.8));
    props.add(new PropertyImpl("EMAIL", lev, 0.3, 0.8));
    
    Configuration config = new ConfigurationImpl();
    ((ConfigurationImpl) config).setProperties(props);
    ((ConfigurationImpl) config).setThreshold(0.85);
    ((ConfigurationImpl) config).setMaybeThreshold(0.7);

    // --- write and reload
    File outfile = tmpdir.newFile("config.xml");
    ConfigWriter writer = new ConfigWriter(new FileOutputStream(outfile.getAbsolutePath()));
    writer.write(config);
    config = ConfigLoader.load(outfile.getAbsolutePath());

    // --- verify loaded correctly    
    assertTrue(config.getDataSources().isEmpty());
    assertTrue(config.getDataSources(1).isEmpty());
    assertTrue(config.getDataSources(2).isEmpty());
    assertEquals(config.getThreshold(), 0.85);
    assertEquals(config.getMaybeThreshold(), 0.7);
    assertEquals(3, config.getProperties().size());

    Property prop = config.getPropertyByName("ID");
    assertTrue("ID property lost", prop.isIdProperty());

    prop = config.getPropertyByName("NAME");
    assertEquals(lev.getClass(), prop.getComparator().getClass());
    assertEquals(0.3, prop.getLowProbability());
    assertEquals(0.8, prop.getHighProbability());

    prop = config.getPropertyByName("EMAIL");
    assertEquals(lev.getClass(), prop.getComparator().getClass());
    assertEquals(0.3, prop.getLowProbability());
    assertEquals(0.8, prop.getHighProbability());
  }

  @Test
  public void testCSV() throws IOException, SAXException {
    // --- build config
    Levenshtein lev = new Levenshtein();
    
    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("ID"));
    props.add(new PropertyImpl("NAME", lev, 0.3, 0.8));
    props.add(new PropertyImpl("EMAIL", lev, 0.3, 0.8));
    
    Configuration config = new ConfigurationImpl();
    ((ConfigurationImpl) config).setProperties(props);
    ((ConfigurationImpl) config).setThreshold(0.85);
    ((ConfigurationImpl) config).setMaybeThreshold(0.7);

    CSVDataSource csv = new CSVDataSource();
    csv.setInputFile("test.csv");
    csv.addColumn(new Column("id", "ID", null, null));
    csv.addColumn(new Column("name", "NAME", null, null));
    Column emailCol = new Column("email", "EMAIL", null, null);
    emailCol.setSplitOn(";");
    csv.addColumn(emailCol);
    ((ConfigurationImpl) config).addDataSource(0, csv);
    
    // --- write and reload
    File outfile = tmpdir.newFile("config.xml");            
    ConfigWriter writer = new ConfigWriter(new FileOutputStream(outfile.getAbsolutePath()));
    writer.write(config);
    config = ConfigLoader.load(outfile.getAbsolutePath());
    
    // --- verify loaded correctly    
    assertEquals(1, config.getDataSources().size());

    csv = (CSVDataSource) config.getDataSources().iterator().next();
    assertTrue(csv.getInputFile().endsWith("test.csv"));
    assertEquals(3, csv.getColumns().size());
    Collection<Column> csvEmailColList = csv.getColumn("email");
    Column csvEmailCol = (Column) csvEmailColList.toArray()[0];
    assertTrue(csvEmailCol.isSplit());
    // FIXME: check the columns (kind of hard given lack of ordering)
    
    assertTrue(config.getDataSources(1).isEmpty());
    assertTrue(config.getDataSources(2).isEmpty());
    assertEquals(config.getThreshold(), 0.85);
    assertEquals(config.getMaybeThreshold(), 0.7);
    assertEquals(3, config.getProperties().size());

    Property prop = config.getPropertyByName("ID");
    assertTrue("ID property lost", prop.isIdProperty());

    prop = config.getPropertyByName("NAME");
    assertEquals(lev.getClass(), prop.getComparator().getClass());
    assertEquals(0.3, prop.getLowProbability());
    assertEquals(0.8, prop.getHighProbability());

    prop = config.getPropertyByName("EMAIL");
    assertEquals(lev.getClass(), prop.getComparator().getClass());
    assertEquals(0.3, prop.getLowProbability());
    assertEquals(0.8, prop.getHighProbability());
  }

}
