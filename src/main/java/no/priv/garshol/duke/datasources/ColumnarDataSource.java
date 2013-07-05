
package no.priv.garshol.duke.datasources;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import no.priv.garshol.duke.Logger;
import no.priv.garshol.duke.DataSource;
import no.priv.garshol.duke.DukeConfigException;

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
}