
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class CSVDataSource extends ColumnarDataSource {
  private String file;
  private String encoding;
  private Reader directreader; // overrides 'file'; used for testing
  private int skiplines;
  private boolean hasheader;

  public CSVDataSource() {
    super();
    this.hasheader = true;
  }

  public void setInputFile(String file) {
    this.file = file;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public void setSkipLines(int skiplines) {
    this.skiplines = skiplines;
  }

  public void setHeaderLine(boolean hasheader) {
    this.hasheader = hasheader;
  }

  // this is used only for testing
  public void setReader(Reader reader) {
    this.directreader = reader;
  }

  public RecordIterator getRecords() {
    if (directreader == null)
      verifyProperty(file, "input-file");
    
    Collection<Record> records = new ArrayList();
    
    try {
      Reader in;
      if (directreader != null)
        in = directreader;
      else {
        if (encoding == null)
          in = new FileReader(file);
        else
          in = new InputStreamReader(new FileInputStream(file), encoding);
      }
      CSVReader reader = new CSVReader(in);
                                                     
      // index here is random 0-n. index[0] gives the column no in the CSV
      // file, while colname[0] gives the corresponding column name.
      int[] index = new int[columns.size()];
      Column[] column = new Column[columns.size()];

      // skip the required number of lines before getting to the data
      for (int ix = 0; ix < skiplines; ix++)
        reader.next();
      
      // learn column indexes from header line (if there is one)
      String[] header;
      if (hasheader)
        header = reader.next();
      else {
        // find highest column number
        int high = 0;
        for (Column c : columns.values())
          high = Math.max(high, Integer.parseInt(c.getName()));
          
        // build corresponding index
        header = new String[high];
        for (int ix = 0; ix < high; ix++)
          header[ix] = "" + (ix + 1);
      }
      
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

  protected String getSourceName() {
    return "CSV";
  }
}