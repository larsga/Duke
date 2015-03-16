
package no.priv.garshol.duke.databases;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import no.priv.garshol.duke.Record;

/**
 * Wraps a Lucene Document to provide a representation of it as a Record.
 */
public class DocumentRecord implements Record {
  /**
   * Beware: this document number will change when changes are made to
   * the Lucene index. So while it's safe to use right now, it is not
   * safe if record objects persist across batch process calls. It
   * might also not be safe in a multi-threaded setting. So
   * longer-term we may need a better solution for removing duplicate
   * candidates.
   */
  private int docno;
  private Document doc;

  public DocumentRecord(int docno, Document doc) {
    this.docno = docno;
    this.doc = doc;
  }
 
  public Collection<String> getProperties() {
    Collection<String> props = new HashSet();
    for (IndexableField f : doc.getFields())
      props.add(f.name());
    return props;
  }
  
  public String getValue(String prop) {
    return doc.get(prop);
  }
 
  public Collection<String> getValues(String prop) {
    IndexableField[] fields = doc.getFields(prop);
    if (fields.length == 1)
      return Collections.singleton(fields[0].stringValue());
    
    Collection<String> values = new ArrayList(fields.length);
    for (int ix = 0; ix < fields.length; ix++)
      values.add(fields[ix].stringValue());
    return values;
  }
  
  public void merge(Record other) {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    return "[DocumentRecord " + docno + " " + doc + "]";
  }

  public int hashCode() {
    return docno;
  }

  public boolean equals(Object other) {
    if (!(other instanceof DocumentRecord))
      return false;

    return ((DocumentRecord) other).docno == docno;
  }
}