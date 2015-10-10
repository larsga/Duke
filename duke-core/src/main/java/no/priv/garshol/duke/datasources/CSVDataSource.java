
package no.priv.garshol.duke.datasources;

import java.util.Collection;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import no.priv.garshol.duke.ConfigWriter;
import no.priv.garshol.duke.DukeConfigException;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.utils.CSVReader;

public class CSVDataSource extends ColumnarDataSource {
  private String file;
  private String encoding;
  private Reader directreader; // overrides 'file'; used for testing
  private int skiplines;
  private boolean hasheader;
  private char separator;
  private int buffer_size;

  public CSVDataSource() {
    super();
    this.hasheader = true;
    this.buffer_size = 65386;
  }

  public String getInputFile() {
    return file;
  }

  public void setInputFile(String file) {
    this.file = file;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public int getSkipLines() {
    return skiplines;
  }

  public void setSkipLines(int skiplines) {
    this.skiplines = skiplines;
  }

  public boolean getHeaderLine() {
    return hasheader;
  }

  public void setHeaderLine(boolean hasheader) {
    this.hasheader = hasheader;
  }

  public char getSeparator() {
    return separator;
  }

  public void setSeparator(char separator) {
    this.separator = separator;
  }

  public int getBufferSize() {
    return buffer_size;
  }

  public void setBufferSize(int buffer_size) {
    this.buffer_size = buffer_size;
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

      CSVReader csv = new CSVReader(in, buffer_size, file);
      if (separator != 0)
        csv.setSeparator(separator);
      return new CSVRecordIterator(csv);
    } catch (FileNotFoundException e) {
      throw new DukeConfigException("Couldn't find CSV file '" + file + "'");
    } catch (IOException e) {
      throw new DukeException(e);
    }
  }

  @Override
  public void writeConfig(ConfigWriter cw) {
    final String name = "csv";
    cw.writeStartElement(name, null);

    cw.writeParam("input-file", getInputFile());
    cw.writeParam("encoding", getEncoding());
    cw.writeParam("skip-lines", getSkipLines());
    cw.writeParam("header-line", getHeaderLine());
    if (getSeparator() != 0)
      cw.writeParam("separator", getSeparator());
    if (getBufferSize() != 65386)
      cw.writeParam("buffer-size", getBufferSize());

    // Write columns
    writeColumnsConfig(cw);

    cw.writeEndElement(name);
  }

  protected String getSourceName() {
    return "CSV";
  }

  public class CSVRecordIterator extends RecordIterator {
    private CSVReader reader;
    private int[] index;     // what index in row to find column[ix] value in
    private Column[] column; // all the columns, in random order
    private RecordBuilder builder;
    private Record nextrecord;

    public CSVRecordIterator(CSVReader reader) throws IOException {
      this.reader = reader;
      this.builder = new RecordBuilder(CSVDataSource.this);

      // using this in case there are more properties than columns (that is,
      // two different properties may come from the same column)
      Collection<Column> allcolumns = getColumns();

      // index here is random 0-n. index[0] gives the column no in the CSV
      // file, while colname[0] gives the corresponding column name.
      index = new int[allcolumns.size()];
      column = new Column[allcolumns.size()];

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

      // what if there is no header?
      if (hasheader && !getColumns().isEmpty() && header == null)
        throw new DukeException("CSV file contained no header");

      // build the 'index' and 'column' indexes
      int count = 0;
      for (Column c : allcolumns) {
        boolean found = false;
        for (int ix = 0; ix < header.length; ix++) {
          if (header[ix].equals(c.getName())) {
            index[count] = ix;
            column[count++] = c;
            found = true;
            break;
          }
        }
        if (!found)
          throw new DukeConfigException("Column " + c.getName() + " not found "+
                                        "in CSV file");
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
          continue; // order is arbitrary, so we might not be done yet

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
