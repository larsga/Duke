
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Previously the default implementation of the Record interface; now
 * superceded by CompactRecord.
 */
public class RecordImpl implements ModifiableRecord {
  private Map<String, Collection<String>> data;

  public RecordImpl(Map<String, Collection<String>> data) {
    this.data = data; // FIXME: should we copy?
  }

  public RecordImpl() {
    this.data = new HashMap();
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }
  
  public Collection<String> getProperties() {
    return data.keySet();
  }
  
  public String getValue(String prop) {
    Collection<String> values = getValues(prop);
    if (values == null || values.isEmpty())
      return null;
    else
      return values.iterator().next();
  }
 
  public Collection<String> getValues(String prop) {
    Collection<String> values = data.get(prop);
    if (values == null)
      return Collections.EMPTY_LIST;
    return values;
  }

  public void addValue(String property, String value) {
    Collection<String> values = data.get(property);
    if (values == null) {
      values = new ArrayList();
      data.put(property, values);
    }
    values.add(value);
  }

  public void remove(String property) {
    data.remove(property);
  }
  
  public void merge(Record other) {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    return "[RecordImpl " + data + "]";
  }
}
