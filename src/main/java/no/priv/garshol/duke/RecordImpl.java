
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

  public void clean(Database database) {
    for (String propname : getProperties()) {
      Property prop = database.getPropertyByName(propname);
      if (prop.isIdProperty() || prop.getCleaner() == null)
        continue;

      Collection<String> dirty = getValues(propname);
      Collection<String> cleaned = new ArrayList(dirty.size());
      for (String value : dirty) {
        value = prop.getCleaner().clean(value);
        if (value != null)
          cleaned.add(value);
      }

      data.put(propname, cleaned);
    }
  }

  public String toString() {
    return "[RecordImpl " + data + "]";
  }
}