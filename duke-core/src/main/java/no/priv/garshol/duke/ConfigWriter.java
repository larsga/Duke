
package no.priv.garshol.duke;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import no.priv.garshol.duke.utils.XMLPrettyPrinter;
import org.xml.sax.helpers.AttributeListImpl;

/**
 * Can write XML configuration files. <b>WARNING</b>: It does not
 * completely preserve all aspects of configurations, so be careful.
 * @since 1.1
 */
public class ConfigWriter {

    private final XMLPrettyPrinter pp;

    public ConfigWriter(OutputStream fos) throws FileNotFoundException, UnsupportedEncodingException {
        pp = new XMLPrettyPrinter(fos);
    }

    /**
   * Writes the given configuration to the given file.
   */
  public void write(Configuration config)
    throws IOException {

    pp.startDocument();
    pp.startElement("duke", null);

    // FIXME: here we should write the objects, but that's not
    // possible with the current API. we don't need that for the
    // genetic algorithm at the moment, but it would be useful.

    pp.startElement("schema", null);

    writeElement("threshold", "" + config.getThreshold());
    if (config.getMaybeThreshold() != 0.0)
      writeElement("maybe-threshold", "" + config.getMaybeThreshold());

    for (Property p : config.getProperties())
      writeProperty(p);

    pp.endElement("schema");

    String dbclass = config.getDatabase(false).getClass().getName();
    AttributeListImpl atts = new AttributeListImpl();
    atts.addAttribute("class", "CDATA", dbclass);
    pp.startElement("database", atts);
    pp.endElement("database");

    if (config.isDeduplicationMode())
      for (DataSource src : config.getDataSources())
        writeDataSource(src);
    else {
      pp.startElement("group", null);
      for (DataSource src : config.getDataSources(1))
        writeDataSource(src);
      pp.endElement("group");

      pp.startElement("group", null);
      for (DataSource src : config.getDataSources(2))
        writeDataSource(src);
      pp.endElement("group");
    }

    pp.endElement("duke");
    pp.endDocument();
  }

  public void writeParam(String name, String value) {
    if (value == null)
      return;

    AttributeListImpl atts = new AttributeListImpl();
    atts.addAttribute("name", "CDATA", name);
    atts.addAttribute("value", "CDATA", value);
    pp.startElement("param", atts);
    pp.endElement("param");
  }

    public void writeParam(String name, int value) {
    writeParam(name, "" + value);
  }

    public void writeParam(String name, char value) {
    writeParam(name, "" + value);
  }

    public void writeParam(String name, boolean value) {
    writeParam(name, "" + value);
  }

  private void writeElement(String name, String value) {
    if (value == null)
      return; // saves us having to repeat these tests everywhere
    pp.startElement(name, null);
    pp.text(value);
    pp.endElement(name);
  }

  private void writeProperty(Property prop) {
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
    writeElement("name", prop.getName());
    if (prop.getComparator() != null)
      writeElement("comparator", prop.getComparator().getClass().getName());
    if (prop.getLowProbability() != 0.0)
      writeElement("low", "" + prop.getLowProbability());
    if (prop.getHighProbability() != 0.0)
      writeElement("high", "" + prop.getHighProbability());
    pp.endElement("property");
  }

  private void writeDataSource(DataSource src) {
    src.writeConfig(this);
  }

  public void writeStartElement(String name, AttributeListImpl atts) {
    pp.startElement(name, atts);
  }

  public void writeEndElement(String name) {
    pp.endElement(name);
  }
}
