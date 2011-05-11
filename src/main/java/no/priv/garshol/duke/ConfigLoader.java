
package no.priv.garshol.duke;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import no.priv.garshol.duke.sdshare.SDshareDataSource;

/**
 * Can read XML configuration files and return a fully set up configuration.
 */
public class ConfigLoader {

  public static Configuration load(String file) {
    try {
      Configuration cfg = new Configuration();

      XMLReader parser = XMLReaderFactory.createXMLReader();
      parser.setContentHandler(new ConfigHandler(cfg));
      parser.parse(file);

      return cfg;
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static class ConfigHandler extends DefaultHandler {
    private Configuration config;
    private Collection<Property> properties;
    private DataSource datasource;
    
    private double threshold;
    private double low;
    private double high;
    private String path;
    private String name;
    private boolean idprop;
    private Comparator comparator;
    
    private Set<String> keepers;
    
    private boolean keep;
    private StringBuffer content;

    private ConfigHandler(Configuration config) {
      this.config = config;
      this.properties = new ArrayList<Property>();
      
      this.keepers = new HashSet();
      this.content = new StringBuffer();

      keepers.add("threshold");
      keepers.add("path");
      keepers.add("name");
      keepers.add("low");
      keepers.add("high");
      keepers.add("comparator");
    }

    public void	startElement(String uri, String localName, String qName,
                             Attributes attributes) {
      if (keepers.contains(localName)) {
        keep = true;
        content.setLength(0); // clear
      } else if (localName.equals("property")) {
        String type = attributes.getValue("type");
        idprop = type != null && type.equals("id");
      } else if (localName.equals("csv"))
        datasource = new CSVDataSource();
      else if (localName.equals("jdbc"))
        datasource = new JDBCDataSource();
      else if (localName.equals("sparql"))
        datasource = new SparqlDataSource();
      else if (localName.equals("ntriples"))
        datasource = new NTriplesDataSource();
      else if (localName.equals("sdshare"))
        datasource = new SDshareDataSource();
       else if (localName.equals("column")) {
        String name = attributes.getValue("name");
        String property = attributes.getValue("property");
        String prefix = attributes.getValue("prefix");
        String cleanername = attributes.getValue("cleaner");
        Cleaner cleaner = null;
        if (cleanername != null)
          cleaner = (Cleaner) instantiate(cleanername);

        if (datasource instanceof ColumnarDataSource)
          ((ColumnarDataSource) datasource).addColumn(
              new Column(name, property, prefix, cleaner));
        else
          throw new RuntimeException("Column inside data source which does " +
                                     "not support it: " + datasource);
      } else if (localName.equals("param"))
        ObjectUtils.setBeanProperty(datasource, attributes.getValue("name"),
                                    attributes.getValue("value"));
    }

    public void characters(char[] ch, int start, int length) {
      if (keep)
        content.append(ch, start, length);
    }

    public void endElement(String uri, String localName, String qName) {
      if (localName.equals("threshold"))
        threshold = Double.parseDouble(content.toString());
      else if (localName.equals("path"))
        path = content.toString();
      else if (localName.equals("name"))
        name = content.toString();
      else if (localName.equals("property")) {
        if (idprop)
          properties.add(new Property(name));
        else
          properties.add(new Property(name, comparator, low, high));
      } else if (localName.equals("low"))
        low = Double.parseDouble(content.toString());
      else if (localName.equals("high"))
        high = Double.parseDouble(content.toString());
      else if (localName.equals("comparator"))
        comparator = (Comparator) instantiate(content.toString());
      else if (localName.equals("csv") ||
               localName.equals("jdbc") ||
               localName.equals("ntriples") ||
               localName.equals("sparql") ||
               localName.equals("sdshare")) {
        config.addDataSource(datasource);
        datasource = null;
      }
      
      if (keepers.contains(localName))
        keep = false;
    }

    public void endDocument() {
      config.setDatabase(new Database(path, properties, threshold, true));
    }
  }  

  private static Object instantiate(String classname) {
    try {
      Class klass = Class.forName(classname);
      return klass.newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}