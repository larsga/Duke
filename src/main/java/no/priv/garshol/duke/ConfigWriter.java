
package no.priv.garshol.duke;

import java.io.IOException;
import java.io.FileOutputStream;

import org.xml.sax.helpers.AttributeListImpl;

import no.priv.garshol.duke.datasources.Column;
import no.priv.garshol.duke.datasources.CSVDataSource;
import no.priv.garshol.duke.datasources.JDBCDataSource;
import no.priv.garshol.duke.datasources.JNDIDataSource;
import no.priv.garshol.duke.datasources.SparqlDataSource;
import no.priv.garshol.duke.datasources.ColumnarDataSource;
import no.priv.garshol.duke.utils.XMLPrettyPrinter;

/**
 * Can write XML configuration files. <b>WARNING</b>: It does not
 * completely preserve all aspects of configurations, so be careful.
 * @since 1.1
 */
public class ConfigWriter {

  /**
   * Writes the given configuration to the given file.
   */
  public static void write(Configuration config, String file)
    throws IOException {
    FileOutputStream fos = new FileOutputStream(file);
    XMLPrettyPrinter pp = new XMLPrettyPrinter(fos);

    pp.startDocument();
    pp.startElement("duke", null);

    // FIXME: here we should write the objects, but that's not
    // possible with the current API. we don't need that for the
    // genetic algorithm at the moment, but it would be useful.

    pp.startElement("schema", null);

    writeElement(pp, "threshold", "" + config.getThreshold());
    if (config.getMaybeThreshold() != 0.0)
      writeElement(pp, "maybe-threshold", "" + config.getMaybeThreshold());

    for (Property p : config.getProperties())
      writeProperty(pp, p);

    pp.endElement("schema");

    String dbclass = config.getDatabase(false).getClass().getName();
    AttributeListImpl atts = new AttributeListImpl();
    atts.addAttribute("class", "CDATA", dbclass);
    pp.startElement("database", atts);
    pp.endElement("database");

    if (config.isDeduplicationMode())
      for (DataSource src : config.getDataSources())
        writeDataSource(pp, src);
    else {
      pp.startElement("group", null);
      for (DataSource src : config.getDataSources(1))
        writeDataSource(pp, src);
      pp.endElement("group");

      pp.startElement("group", null);
      for (DataSource src : config.getDataSources(2))
        writeDataSource(pp, src);
      pp.endElement("group");
    }

    pp.endElement("duke");
    pp.endDocument();

    fos.close();
  }

  private static void writeParam(XMLPrettyPrinter pp, String name, String value) {
    if (value == null)
      return;

    AttributeListImpl atts = new AttributeListImpl();
    atts.addAttribute("name", "CDATA", name);
    atts.addAttribute("value", "CDATA", value);
    pp.startElement("param", atts);
    pp.endElement("param");
  }

  private static void writeParam(XMLPrettyPrinter pp, String name, int value) {
    writeParam(pp, name, "" + value);
  }

  private static void writeParam(XMLPrettyPrinter pp, String name, char value) {
    writeParam(pp, name, "" + value);
  }

  private static void writeParam(XMLPrettyPrinter pp, String name, boolean value) {
    writeParam(pp, name, "" + value);
  }

  private static void writeElement(XMLPrettyPrinter pp, String name, String value) {
    if (value == null)
      return; // saves us having to repeat these tests everywhere
    pp.startElement(name, null);
    pp.text(value);
    pp.endElement(name);
  }

  private static void writeProperty(XMLPrettyPrinter pp, Property prop) {
    AttributeListImpl atts = new AttributeListImpl();
    if (prop.isIdProperty())
      atts.addAttribute("type", "CDATA", "id");
    else if (prop.isIgnoreProperty())
      atts.addAttribute("type", "CDATA", "ignore");

    if (!prop.isIdProperty() &&
        prop.getLookupBehaviour() != Property.Lookup.DEFAULT) {
      String value = prop.getLookupBehaviour().toString().toLowerCase();
      atts.addAttribute("lookup", "CDATA", value);
    }

    pp.startElement("property", atts);
    writeElement(pp, "name", prop.getName());
    if (prop.getComparator() != null)
      writeElement(pp, "comparator", prop.getComparator().getClass().getName());
    if (prop.getLowProbability() != 0.0)
      writeElement(pp, "low", "" + prop.getLowProbability());
    if (prop.getHighProbability() != 0.0)
      writeElement(pp, "high", "" + prop.getHighProbability());
    pp.endElement("property");
  }

  private static void writeDataSource(XMLPrettyPrinter pp, DataSource src) {
    String name = null;
    if (src instanceof JNDIDataSource) {
      name = "jndi";
      JNDIDataSource jndi = (JNDIDataSource) src;
      pp.startElement(name, null);

      writeParam(pp, "jndi-path", jndi.getJndiPath());
      writeParam(pp, "query", jndi.getQuery());
    } else if (src instanceof JDBCDataSource) {
      name = "jdbc";
      JDBCDataSource jdbc = (JDBCDataSource) src;
      pp.startElement(name, null);

      writeParam(pp, "driver-class", jdbc.getDriverClass());
      writeParam(pp, "connection-string", jdbc.getConnectionString());
      writeParam(pp, "user-name", jdbc.getUserName());
      writeParam(pp, "password", jdbc.getPassword());
      writeParam(pp, "query", jdbc.getQuery());
    } else if (src instanceof CSVDataSource) {
      name = "csv";
      CSVDataSource csv = (CSVDataSource) src;
      pp.startElement(name, null);

      writeParam(pp, "input-file", csv.getInputFile());
      writeParam(pp, "encoding", csv.getEncoding());
      writeParam(pp, "skip-lines", csv.getSkipLines());
      writeParam(pp, "header-line", csv.getHeaderLine());
      if (csv.getSeparator() != 0)
        writeParam(pp, "separator", csv.getSeparator());
    } else if (src instanceof SparqlDataSource) {
      name = "sparql";
      SparqlDataSource sparql = (SparqlDataSource) src;
      pp.startElement(name, null);

      writeParam(pp, "endpoint", sparql.getEndpoint());
      writeParam(pp, "query", sparql.getQuery());
      writeParam(pp, "page-size", sparql.getPageSize());
      writeParam(pp, "triple-mode", sparql.getTripleMode());
    }

    if (src instanceof ColumnarDataSource) {
      // FIXME: this breaks the order...
      for (Column col : ((ColumnarDataSource) src).getColumns()) {
        AttributeListImpl atts = new AttributeListImpl();
        atts.addAttribute("name", "CDATA", col.getName());
        atts.addAttribute("property", "CDATA", col.getProperty());
        if (col.getPrefix() != null)
          atts.addAttribute("prefix", "CDATA", col.getPrefix());
        // FIXME: cleaner really requires object support ... :-(
        if (col.getCleaner() != null)
          atts.addAttribute("cleaner", "CDATA", col.getCleaner().getClass().getName());
        pp.startElement("column", atts);
        pp.endElement("column");
      }
    }

    pp.endElement(name);
  }
}
