
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashSet;
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
  private Collection<String> types;

  public NTriplesDataSource() {
    super();
    this.types = new HashSet();
  }

  public void setInputFile(String file) {
    this.file = file;
  }

  public void setAcceptTypes(String types) {
    // FIXME: accept more than one
    this.types.add(types);
  }

  public RecordIterator getRecords() {
    verifyProperty(file, "input-file");
    
    try {
      RecordBuilder builder = new RecordBuilder(types);
      NTriplesParser.parse(new InputStreamReader(new FileInputStream(file),
                                                 "utf-8"), builder);
      builder.filterByTypes();
      Iterator it = builder.getRecords().values().iterator();
      return new DefaultRecordIterator(it);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected String getSourceName() {
    return "NTriples";
  }

  // ----- handler

  private static final String RDF_TYPE =
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
  
  class RecordBuilder implements NTriplesParser.StatementHandler {
    private Map<String, RecordImpl> records;
    private Collection<String> types;

    public RecordBuilder(Collection<String> types) {
      this.records = new HashMap();
      this.types = types;
    }

    public void filterByTypes() {
      // this is fairly ugly. if types has values we add an extra property
      // RDF_TYPE to records during build, then filter out records of the
      // wrong types here. finally, we strip away the RDF_TYPE property here.
      
      if (types.isEmpty())
        return;
      
      for (String uri : new ArrayList<String>(records.keySet())) {
        RecordImpl r = records.get(uri);
        boolean found = false;
        for (String value : r.getValues(RDF_TYPE))
          if (types.contains(value))
            found = true;
        if (!found)
          records.remove(uri);
        else
          r.remove(RDF_TYPE);
      }
    }

    public Map<String, RecordImpl> getRecords() {
      return records;
    }

    public void statement(String subject, String property, String object,
                          boolean literal) {
      Column col = columns.get(property);
      String theprop;
      
      if (col != null) {
        if (col.getCleaner() != null)
          object = col.getCleaner().clean(object);
        theprop = col.getProperty();
      } else if (property.equals(RDF_TYPE) && !types.isEmpty())
        theprop = RDF_TYPE;
      else
        return;

      if (object == null || object.equals(""))
        return; // nothing here, move on

      RecordImpl record = records.get(subject);
      if (record == null) {
        record = new RecordImpl();
        records.put(subject, record);

        Column idcol = columns.get("?uri");
        record.addValue(idcol.getProperty(), subject);
      }
      record.addValue(theprop, object);      
    }
  }
}