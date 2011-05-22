
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;

public abstract class ColumnarDataSource implements DataSource {
  protected Map<String, Column> columns;
  protected Logger logger;

  public ColumnarDataSource() {
    this.columns = new HashMap();
  }

  public void addColumn(Column column) {
    columns.put(column.getName(), column);
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