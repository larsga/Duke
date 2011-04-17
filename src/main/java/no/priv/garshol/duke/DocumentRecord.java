
package no.priv.garshol.duke;

import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

/**
 * Wraps a Lucene Document to provide a representation of it as a Record.
 */
public class DocumentRecord implements Record {
  private Document doc;

  public DocumentRecord(Document doc) {
    this.doc = doc;
  }
  
  public Collection<String> getIdentities() {
    return Collections.EMPTY_SET;
  }

  public Collection<String> getProperties() {
    Collection<String> props = new HashSet();
    for (Fieldable f : doc.getFields())
      props.add(f.name());
    return props;
  }
  
  public String getValue(String prop) {
    return doc.get(prop);
  }
 
  public Collection<String> getValues(String prop) {
    String v = getValue(prop);
    if (v == null)
      return Collections.EMPTY_SET;
    return Collections.singleton(v);
  }
  
  public void merge(Record other) {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    return "[DocumentRecord " + doc + "]";
  }
}