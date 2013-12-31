
package no.priv.garshol.duke.datasources;

import java.util.Collection;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Cleaner;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.CompactRecord;
import no.priv.garshol.duke.ModifiableRecord;

/**
 * Helper class for building records, to avoid having to copy all the
 * cleaning logic etc in each single data source.
 */
public class RecordBuilder {
  private ColumnarDataSource source;
  private ModifiableRecord record;

  public RecordBuilder(ColumnarDataSource source) {
    this.source = source;
  }

  public void newRecord() {
    record = new CompactRecord();
  }

  public boolean isRecordEmpty() {
    return record.isEmpty();
  }
    
  public void addValue(String column, String value) {
    Collection<Column> cols = source.getColumn(column);
    if (cols == null || cols.isEmpty())
      return;
    Column col = cols.iterator().next();
    addValue(col, value);
  }

  public void addValue(Column col, String value) {
    if (value == null || value.equals(""))
      return;
    
    String prop = col.getProperty();
    Cleaner cleaner = col.getCleaner();
    if (col.isSplit()) {
      for (String v : col.split(value)) {
        if (cleaner != null)
          v = cleaner.clean(v);
        if (v != null && !v.equals(""))
          record.addValue(prop, v);
      }
    } else {
      if (cleaner != null)
        value = cleaner.clean(value);
      if (value != null && !value.equals(""))
        record.addValue(prop, value);
    }
  }

  // FIXME: probably we should just get rid of these
  public void setValue(String column, String value) {
    Collection<Column> cols = source.getColumn(column);
    Column col = cols.iterator().next();
    setValue(col, value);
  }

  public void setValue(Column col, String value) {
    if (col.getCleaner() != null)
      value = col.getCleaner().clean(value);
    if (value == null || value.equals(""))
      return; // nothing here, move on
    
    record.addValue(col.getProperty(), value);
  }
  
  public Record getRecord() {
    return record;
  }
}
