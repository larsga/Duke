
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.FileReader;
import java.io.IOException;

public class CSVDataSource implements DataSource {
  private String file;
  private Map<String, Column> columns;

  public CSVDataSource() {
    this.columns = new HashMap();
  }

  public void addColumn(Column column) {
    columns.put(column.getName(), column);
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public Iterator<Record> getRecords() {
    Collection<Record> records = new ArrayList();
    
    try {
      // FIXME: encoding
      CSVReader reader = new CSVReader(new FileReader(file));

      // index here is random 0-n. index[0] gives the column no in the CSV
      // file, while colname[0] gives the corresponding column name.
      int[] index = new int[columns.size()];
      Column[] column = new Column[columns.size()];

      // learn column indexes from header line
      int count = 0;
      String[] header = reader.next();      
      for (int ix = 0; ix < header.length && count < index.length; ix++)
        if (columns.containsKey(header[ix])) {
          column[count] = columns.get(header[ix]);
          index[count++] = ix;
        }
      
      // build records      
      while (true) {
        String[] row = reader.next();
        if (row == null)
          break;

        Map<String, Collection<String>> values = new HashMap();
        for (int ix = 0; ix < column.length; ix++) {
          if (index[ix] >= row.length)
            break;
          
          Column col = column[ix];
          String value = row[index[ix]];
          if (col.getCleaner() != null)
            value = col.getCleaner().clean(value);
          if (col.getPrefix() != null)
            value = col.getPrefix() + value;

          String propname = col.getProperty();
          values.put(propname, Collections.singleton(value));          
        }
        records.add(new RecordImpl(values));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return records.iterator();
  }
}