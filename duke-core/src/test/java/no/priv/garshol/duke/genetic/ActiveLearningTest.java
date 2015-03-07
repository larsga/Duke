
package no.priv.garshol.duke.genetic;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;
import org.junit.Rule;
import org.junit.Before;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.rules.TemporaryFolder;

import no.priv.garshol.duke.Comparator;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.PropertyImpl;
import no.priv.garshol.duke.ConfigurationImpl;
import no.priv.garshol.duke.databases.InMemoryDatabase;
import no.priv.garshol.duke.comparators.ExactComparator;
import no.priv.garshol.duke.datasources.Column;
import no.priv.garshol.duke.datasources.CSVDataSource;

public class ActiveLearningTest {
  @Rule
  public TemporaryFolder tmpdir = new TemporaryFolder();

  @Test
  public void testSmallData() throws IOException {
    File outfile = tmpdir.newFile("test.csv");

    FileWriter out = new FileWriter(outfile);
    out.write("id;name;age\n");
    out.write("1;LMG;39\n");
    out.write("2;GOG;40\n");
    out.write("3;GDM;29\n");
    out.write("4;AB;49\n");
    out.close();

    File tstfile = tmpdir.newFile("testfile.csv");
    out = new FileWriter(tstfile);
    out.close();

    CSVDataSource csv = new CSVDataSource();
    csv.setSeparator(';');
    csv.setInputFile(outfile.getAbsolutePath());
    csv.addColumn(new Column("id", null, null, null));
    csv.addColumn(new Column("name", null, null, null));
    csv.addColumn(new Column("age", null, null, null));

    ConfigurationImpl cfg = new ConfigurationImpl();
    cfg.addDatabase(new InMemoryDatabase());
    cfg.addDataSource(0, csv);

    Comparator cmp = new ExactComparator();

    List<Property> props = new ArrayList();
    props.add(new PropertyImpl("id"));
    props.add(new PropertyImpl("name", cmp, 0.0, 1.0));
    props.add(new PropertyImpl("age", cmp, 0.0, 1.0));

    cfg.setProperties(props);

    GeneticAlgorithm gen = new GeneticAlgorithm(cfg, tstfile.getAbsolutePath(),
                                                true);
    gen.setQuiet(true);
    gen.run(); // should not crash!
  }

}
