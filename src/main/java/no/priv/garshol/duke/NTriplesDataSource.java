
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * This is a naive data source which keeps all data in memory. If we
 * knew that the NTriples file was sorted on subject we could do
 * better, but this source doesn't do that.
 */
public class NTriplesDataSource extends ColumnarDataSource {
  private String file;

  public NTriplesDataSource() {
    super();
  }

  public void setInputFile(String file) {
    this.file = file;
  }

  public RecordIterator getRecords() {    
    try {
      RecordBuilder builder = new RecordBuilder();
      NTriplesParser.parse(new InputStreamReader(new FileInputStream(file),
                                                 "utf-8"), builder);
      Iterator it = builder.getRecords().values().iterator();
      return new DefaultRecordIterator(it);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // ----- handler

  class RecordBuilder implements NTriplesParser.StatementHandler {
    private Map<String, RecordImpl> records;

    public RecordBuilder() {
      this.records = new HashMap();
    }

    public Map<String, RecordImpl> getRecords() {
      return records;
    }

    public void statement(String subject, String property, String object,
                          boolean literal) {
      Column col = columns.get(property);
      if (col == null)
        return;
      if (col.getCleaner() != null)
        object = col.getCleaner().clean(object);
      if (object == null || object.equals(""))
        return; // nothing here, move on

      RecordImpl record = records.get(subject);
      if (record == null) {
        record = new RecordImpl();
        records.put(subject, record);

        Column idcol = columns.get("?uri");
        record.addValue(idcol.getProperty(), subject);
      }
      record.addValue(col.getProperty(), object);      
    }
  }
}