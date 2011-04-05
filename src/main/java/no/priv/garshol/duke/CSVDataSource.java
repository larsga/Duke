
package no.priv.garshol.duke;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class CSVDataSource implements DataSource {
  private String file;
  private Collection<Column> columns;

  public CSVDataSource() {
    this.columns = new ArrayList();
  }

  public void addColumn(Column column) {
    columns.add(column);
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public Iterator<Record> getRecords() {
    return Collections.EMPTY_SET.iterator();
  }
  
}