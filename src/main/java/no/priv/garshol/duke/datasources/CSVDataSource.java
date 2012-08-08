
package no.priv.garshol.duke.datasources;

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
import java.io.FileNotFoundException;
import java.io.IOException;

import no.priv.garshol.duke.Column;
import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.utils.CSVReader;

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

      return new CSVRecordIterator(new CSVReader(in));
    } catch (FileNotFoundException e) {
      throw new DukeConfigException("Couldn't find CSV file '" + file + "'");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }    
  }

  protected String getSourceName() {
    return "CSV";
  }

  public class CSVRecordIterator extends RecordIterator {
    private CSVReader reader;
    private int[] index;     // what index in row to find colum[ix] value in
    private Column[] column; // all the columns, in random order
    private RecordBuilder builder;
    private Record nextrecord;
    
    public CSVRecordIterator(CSVReader reader) throws IOException {
      this.reader = reader;
      this.builder = new RecordBuilder(CSVDataSource.this);

      // index here is random 0-n. index[0] gives the column no in the CSV
      // file, while colname[0] gives the corresponding column name.
      index = new int[columns.size()];
      column = new Column[columns.size()];

      // skip the required number of lines before getting to the data
      for (int ix = 0; ix < skiplines; ix++)
        reader.next();
      
      // learn column indexes from header line (if there is one)
      String[] header = null;
      if (hasheader)
        header = reader.next();
      else {
        // find highest column number
        int high = 0;
        for (Column c : getColumns())
          high = Math.max(high, Integer.parseInt(c.getName()));
          
        // build corresponding index
        header = new String[high];
        for (int ix = 0; ix < high; ix++)
          header[ix] = "" + (ix + 1);
      }

      // build the 'index' and 'column' indexes
      int count = 0;
      for (Column c : getColumns()) {
        for (int ix = 0; ix < header.length; ix++) {
          if (header[ix].equals(c.getName())) {
            index[count] = ix;
            column[count++] = c;
            break;
          }
        }
      }

      findNextRecord();
    }
    
    private void findNextRecord() {
      String[] row;
      try {
        row = reader.next();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      
      if (row == null) {
        nextrecord = null; // there isn't any next record
        return;
      }

      // build a record from the current row
      builder.newRecord();
      for (int ix = 0; ix < column.length; ix++) {
        if (index[ix] >= row.length)
          break;

        builder.addValue(column[ix], row[index[ix]]);
      }

      nextrecord = builder.getRecord();
    }

    public boolean hasNext() {
      return (nextrecord != null);
    }

    public Record next() {
      Record thenext = nextrecord;
      findNextRecord();
      return thenext;
    }
  }  
}