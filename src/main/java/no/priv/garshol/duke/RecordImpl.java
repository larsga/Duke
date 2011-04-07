
package no.priv.garshol.duke;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Default implementation. Possibly the only ever. Not sure yet.
 */
public class RecordImpl implements Record {
  private Map<String, Collection<String>> data;

  public RecordImpl(Map<String, Collection<String>> data) {
    this.data = data; // FIXME: should we copy?
  }

  public Collection<String> getProperties() {
    return data.keySet();
  }
  
  public Collection<String> getIdentities() {
    return Collections.EMPTY_SET; // FIXME
  }
  
  public String getValue(String prop) {
    Collection<String> values = getValues(prop);
    if (values == null || values.isEmpty())
      return null;
    else
      return values.iterator().next();
  }
 
  public Collection<String> getValues(String prop) {
    return data.get(prop);
  }
  
  public void merge(Record other) {
  }

  public String toString() {
    return "[RecordImpl " + data + "]";
  }
}