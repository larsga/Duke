
package no.priv.garshol.duke.datasources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import no.priv.garshol.duke.ConfigWriter;
import no.priv.garshol.duke.DataSource;
import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.Logger;
import org.xml.sax.helpers.AttributeListImpl;

/**
 * Abstract class for sharing code that is common to column-based data
 * sources.
 */
public abstract class ColumnarDataSource implements DataSource {
  protected Map<String, Collection<Column>> columns;
  protected Logger logger;

  public ColumnarDataSource() {
    this.columns = new HashMap();
  }

  public void addColumn(Column column) {
    Collection<Column> cols = columns.get(column.getName());
    if (cols == null) {
      cols = new ArrayList();
      columns.put(column.getName(), cols);
    }
    cols.add(column);
  }

  public Collection<Column> getColumn(String name) {
    return columns.get(name);
  }

  public Collection<Column> getColumns() {
    Collection<Column> all = new ArrayList(columns.size());
    for (Collection<Column> col : columns.values())
      all.addAll(col);
    return all;
  }

  public void setLogger(Logger logger) {
    this.logger = logger;
  }

  protected abstract String getSourceName();

  protected void verifyProperty(String value, String name) {
    if (value == null)
      throw new DukeConfigException("Missing '" + name + "' property to " +
          getSourceName() + " data source");
  }

  protected void writeColumnsConfig(ConfigWriter cw) {
    // FIXME: this breaks the order...
    for (Column col : getColumns()) {
      AttributeListImpl atts = new AttributeListImpl();
      atts.addAttribute("name", "CDATA", col.getName());
      atts.addAttribute("property", "CDATA", col.getProperty());
      if (col.getPrefix() != null)
        atts.addAttribute("prefix", "CDATA", col.getPrefix());
      // FIXME: cleaner really requires object support ... :-(
      if (col.getCleaner() != null)
        atts.addAttribute("cleaner", "CDATA", col.getCleaner().getClass().getName());
      if (col.isSplit())
        atts.addAttribute("split-on", "CDATA", col.getSplitOn());

      cw.writeStartElement("column", atts);
      cw.writeEndElement("column");
    }
  }
}