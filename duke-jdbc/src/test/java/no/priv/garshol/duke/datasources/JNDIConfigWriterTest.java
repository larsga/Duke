
package no.priv.garshol.duke.datasources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import no.priv.garshol.duke.ConfigLoader;
import no.priv.garshol.duke.ConfigWriter;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.comparators.Levenshtein;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class JNDIConfigWriterTest {
  @Rule
  public TemporaryFolder tmpdir = new TemporaryFolder();

  @Test
  public void testJNDI() throws IOException, SAXException {
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

    JNDIDataSource jndi = new JNDIDataSource();
    jndi.setJndiPath("path");
    jndi.setQuery("select");
    jndi.addColumn(new Column("id", "ID", null, null));
    jndi.addColumn(new Column("name", "NAME", null, null));
    jndi.addColumn(new Column("email", "EMAIL", null, null));
    ((ConfigurationImpl) config).addDataSource(0, jndi);
    
    // --- write and reload
    File outfile = tmpdir.newFile("config.xml");
    ConfigWriter writer = new ConfigWriter(new FileOutputStream(outfile.getAbsolutePath()));
    writer.write(config);
    config = ConfigLoader.load(outfile.getAbsolutePath());
    
    // --- verify loaded correctly    
    assertEquals(1, config.getDataSources().size());

    jndi = (JNDIDataSource) config.getDataSources().iterator().next();
    assertEquals("select", jndi.getQuery());
    assertEquals(3, jndi.getColumns().size());
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
