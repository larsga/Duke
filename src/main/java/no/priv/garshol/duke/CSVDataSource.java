
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.FileReader;
import java.io.IOException;

public class CSVDataSource extends ColumnarDataSource {
  private String file;

  public CSVDataSource() {
    super();
  }

  public void setInputFile(String file) {
    this.file = file;
  }

  public RecordIterator getRecords() {
    Collection<Record> records = new ArrayList();
    
    try {
      // FIXME: encoding
      CSVReader reader = new CSVReader(new FileReader(file));

      // index here is random 0-n. index[0] gives the column no in the CSV
      // file, while colname[0] gives the corresponding column name.
      int[] index = new int[columns.size()];
      Column[] column = new Column[columns.size()];

      // learn column indexes from header line
      String[] header = reader.next();
      int count = 0;
      for (Column c : columns.values()) {
        for (int ix = 0; ix < header.length; ix++) {
          if (header[ix].equals(c.getName())) {
            index[count] = ix;
            column[count++] = c;
            break;
          }
        }
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
          if (value == null || value.equals(""))
            continue; // nothing here, move on
          
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

    return new DefaultRecordIterator(records.iterator());
  }
}