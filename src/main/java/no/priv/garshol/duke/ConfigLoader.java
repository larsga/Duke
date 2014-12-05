
package no.priv.garshol.duke;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.io.File;
import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.utils.StringUtils;
import no.priv.garshol.duke.utils.ObjectUtils;
import no.priv.garshol.duke.cleaners.ChainedCleaner;
import no.priv.garshol.duke.comparators.ExactComparator;
import no.priv.garshol.duke.datasources.Column;
import no.priv.garshol.duke.datasources.CSVDataSource;
import no.priv.garshol.duke.datasources.ColumnarDataSource;
import no.priv.garshol.duke.datasources.JDBCDataSource;
import no.priv.garshol.duke.datasources.JNDIDataSource;
import no.priv.garshol.duke.datasources.NTriplesDataSource;
import no.priv.garshol.duke.datasources.SparqlDataSource;

/**
 * Can read XML configuration files and return a fully set up configuration.
 */
public class ConfigLoader {
  private static Logger logger = new DummyLogger();

  /**
   * Note that if file starts with 'classpath:' the resource is looked
   * up on the classpath instead.
   */
  public static Configuration load(String file)
    throws IOException, SAXException {
    ConfigurationImpl cfg = new ConfigurationImpl();

    XMLReader parser = XMLReaderFactory.createXMLReader();
    parser.setContentHandler(new ConfigHandler(cfg, file));
    if (file.startsWith("classpath:")) {
      String resource = file.substring("classpath:".length());
      ClassLoader cloader = Thread.currentThread().getContextClassLoader();
      InputStream istream = cloader.getResourceAsStream(resource);
      parser.parse(new InputSource(istream));
    } else
      parser.parse(file);

    return cfg;
  }

  /**
   * Loads the configuration XML from the given string.
   * @since 1.3
   */
  public static Configuration loadFromString(String config)
    throws IOException, SAXException {
    ConfigurationImpl cfg = new ConfigurationImpl();

    XMLReader parser = XMLReaderFactory.createXMLReader();
    parser.setContentHandler(new ConfigHandler(cfg, null));
    Reader reader = new StringReader(config);
    parser.parse(new InputSource(reader));
    return cfg;
  }

  private static class ConfigHandler extends DefaultHandler {
    private ConfigurationImpl config;
    private List<Property> properties;
    private List<Comparator> customComparators;
    private File path; // location of config file

    private double low;
    private double high;
    private String name;
    private boolean idprop;
    private boolean ignore_prop;
    private Comparator comparator;
    private Property.Lookup lookup;

    private Set<String> keepers;
    private int groupno; // counts datasource groups
    private Map<String, Object> objects; // configured Java beans for reuse
    private DataSource datasource;
    private Object currentobj; // Java bean currently being configured by <param>
    private Database database;

    private boolean keep;
    private StringBuffer content;

    private ConfigHandler(ConfigurationImpl config, String path) {
      this.config = config;
      this.properties = new ArrayList<Property>();
      if (path != null && !path.startsWith("classpath:"))
        this.path = new File(path).getParentFile();

      this.objects = new HashMap();
      this.keepers = new HashSet();
      this.content = new StringBuffer();

      keepers.add("threshold");
      keepers.add("maybe-threshold");
      keepers.add("name");
      keepers.add("low");
      keepers.add("high");
      keepers.add("comparator");
    }

    private void checkAttributes(String localName, Attributes attributes, String... usedAttributes) {
      Set<String> attrSet = new HashSet<String>();
      for (int i = 0 ; i < attributes.getLength() ; i++) {
        attrSet.add(attributes.getLocalName(i));
      }
      for (String attr : usedAttributes) {
        attrSet.remove(attr);
      }
      if (!attrSet.isEmpty()) {
        logger.warn("The following attributes of <"+ localName +"> are ignored: " + attrSet);
      }
    }

    public void	startElement(String uri, String localName, String qName,
                             Attributes attributes) {
      if (keepers.contains(localName)) {
        keep = true;
        content.setLength(0); // clear
      } else if (localName.equals("property")) {
        String type = attributes.getValue("type");
        idprop = type != null && type.equals("id");
        ignore_prop = type != null && type.equals("ignore");
        low = 0.5;
        high = 0.5;
        comparator = null;
        lookup = Property.Lookup.DEFAULT;
        if (attributes.getValue("lookup") != null)
          lookup = (Property.Lookup) ObjectUtils.getEnumConstantByName(
                                Property.Lookup.class,
                                attributes.getValue("lookup").toUpperCase());

        checkAttributes("property" ,attributes, "type", "lookup");
      } else if (localName.equals("csv")) {
        datasource = new CSVDataSource();
        currentobj = datasource;
        checkAttributes("csv", attributes);
      } else if (localName.equals("jdbc")) {
        datasource = new JDBCDataSource();
        currentobj = datasource;
        checkAttributes("jdbc", attributes);
      } else if (localName.equals("jndi")) {
        datasource = new JNDIDataSource();
        currentobj = datasource;
        checkAttributes("jndi", attributes);
      } else if (localName.equals("sparql")) {
        datasource = new SparqlDataSource();
        currentobj = datasource;
        checkAttributes("sparql", attributes);
      } else if (localName.equals("ntriples")) {
        datasource = new NTriplesDataSource();
        currentobj = datasource;
        checkAttributes("ntriples", attributes);
      } else if (localName.equals("data-source")) {
        datasource = (DataSource) instantiate(attributes.getValue("class"));
        currentobj = datasource;
        checkAttributes("data-source", attributes);
      } else if (localName.equals("column")) {
        if (!(datasource instanceof ColumnarDataSource))
          throw new DukeConfigException("Column inside data source which " +
                                        "does not support it: " + datasource);

        String name = attributes.getValue("name");
        if (name == null)
          throw new DukeConfigException("Column with no name");
        String property = attributes.getValue("property");
        String prefix = attributes.getValue("prefix");
        String cleanername = attributes.getValue("cleaner");
        Cleaner cleaner = makeCleaner(cleanername);

        Column c = new Column(name, property, prefix, cleaner);
        String spliton = attributes.getValue("split-on");
        if (spliton != null)
          c.setSplitOn(spliton);

        ((ColumnarDataSource) datasource).addColumn(c);
        checkAttributes("column", attributes, "name", "property", "prefix", "cleaner", "split-on");
      } else if (localName.equals("param")) {
        String param = attributes.getValue("name");
        String value = attributes.getValue("value");

        if (currentobj == null)
          throw new DukeConfigException("Trying to set parameter " +
                                        param + " but no current object");

        // we resolve file references relative to the config file location
        if (param.equals("input-file") && path != null &&
            !value.startsWith("/"))
          value = new File(path, value).getAbsolutePath();

        ObjectUtils.setBeanProperty(currentobj, param, value, objects);

        checkAttributes("param", attributes, "name", "value");
      } else if (localName.equals("group")) {
        groupno++;
        // FIXME: now possible to have data sources between the two
        // groups.  need to check for that, too. ideally XML
        // validation should take care of all this for us.
        if (groupno == 1 && !config.getDataSources().isEmpty())
          throw new DukeConfigException("Cannot have groups in deduplication mode");
        else if (groupno == 3)
          throw new DukeConfigException("Record linkage mode only supports " +
                                        "two groups");

        checkAttributes("param", attributes);
      } else if (localName.equals("object")) {
        String klass = attributes.getValue("class");
        String name = attributes.getValue("name");
        currentobj = instantiate(klass);
        objects.put(name, currentobj);
        checkAttributes("param", attributes, "name", "class");
      } else if (localName.equals("database")) {
        String klass = attributes.getValue("class");
        if (klass == null)
          klass = "no.priv.garshol.duke.LuceneDatabase"; // default
        database = (Database) instantiate(klass);
        currentobj = database;
        checkAttributes("param", attributes, "class");
      }
    }

    public void characters(char[] ch, int start, int length) {
      if (keep)
        content.append(ch, start, length);
    }

    public void endElement(String uri, String localName, String qName) {
      if (localName.equals("threshold"))
        config.setThreshold(Double.parseDouble(content.toString()));
      else if (localName.equals("maybe-threshold"))
        config.setMaybeThreshold(Double.parseDouble(content.toString()));
      else if (localName.equals("name"))
        name = content.toString();
      else if (localName.equals("property")) {
        if (idprop)
          properties.add(new PropertyImpl(name));
        else {
          Property p = new PropertyImpl(name, comparator, low, high);
          if (ignore_prop)
            p.setIgnoreProperty(true);
          p.setLookupBehaviour(lookup);
          properties.add(p);
        }
      } else if (localName.equals("low"))
        low = Double.parseDouble(content.toString());
      else if (localName.equals("high"))
        high = Double.parseDouble(content.toString());
      else if (localName.equals("comparator")) {
        comparator = (Comparator) objects.get(content.toString());
        if (comparator == null) // wasn't a configured bean
          comparator = (Comparator) instantiate(content.toString());

      } else if (localName.equals("csv") ||
               localName.equals("jdbc") ||
               localName.equals("jndi") ||
               localName.equals("ntriples") ||
               localName.equals("sparql") ||
               localName.equals("data-source")) {
        config.addDataSource(groupno, datasource);
        datasource = null;
        currentobj = null;
      } else if (localName.equals("object")) {
        if (currentobj instanceof Comparator)
          // store custom comparators so genetic algorithm can get them
          config.addCustomComparator((Comparator) currentobj);
        currentobj = null;
      }
      else if (localName.equals("database"))
        config.addDatabase(database);

      if (keepers.contains(localName))
        keep = false;

      else if (localName.equals("duke")) {
        if (groupno > 0 && groupno != 2)
          throw new DukeConfigException("Record linkage mode requires exactly 2 groups; should you be using deduplication mode?");
      }
    }

    public void endDocument() {
      config.setProperties(properties);
    }

    private Cleaner makeCleaner(String value) {
      if (value == null)
        return null;

      String[] names = StringUtils.split(value);
      Cleaner[] cleaners = new Cleaner[names.length];
      for (int ix = 0; ix < cleaners.length; ix++)
        cleaners[ix] = _makeCleaner(names[ix]);

      if (cleaners.length == 1)
        return cleaners[0];
      else
        return new ChainedCleaner(cleaners);
    }

    private Cleaner _makeCleaner(String name) {
      Cleaner cleaner = (Cleaner) objects.get(name);
      if (cleaner == null) // wasn't a configured bean
        cleaner = (Cleaner) instantiate(name);
      return cleaner;
    }
  }

  private static Object instantiate(String classname) {
    try {
      Class klass = Class.forName(classname);
      return klass.newInstance();
    }
    catch (Exception e) {
      throw new DukeConfigException("Couldn't instantiate class " + classname +
                                    ": " + e);
    }
  }

  /**
   * Sets the logger to report to.
   */
  public static void setLogger(Logger logger_) {
    logger = logger_;
  }

}
