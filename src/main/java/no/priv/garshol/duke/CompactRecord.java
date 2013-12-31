
package no.priv.garshol.duke;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;

public class CompactRecord implements ModifiableRecord {
  private String[] s; // 0: prop name, 1: value, 2: prop, 3: value, ...
  private int free; // index of next free prop name cell

  public CompactRecord() {
    this.s = new String[16];
  }
  
  public Collection<String> getProperties() {
    Collection<String> props = new HashSet();
    for (int ix = 0; ix < free; ix += 2)
      props.add(s[ix]);
    return props;
  }
  
  public Collection<String> getValues(String prop) {
    Collection<String> values = new ArrayList();
    for (int ix = 0; ix < free; ix += 2)
      if (s[ix].equals(prop))
        values.add(s[ix + 1]);
    return values;
  }

  public String getValue(String prop) {
    for (int ix = 0; ix < free; ix += 2)
      if (s[ix].equals(prop))
        return s[ix + 1];
    return null;
  }
  
  public void merge(Record other) {
    throw new UnsupportedOperationException();
  }

  public void addValue(String property, String value) {
    if (free > s.length) {
      String[] olds = s;
      s = new String[olds.length * 3];
      for (int ix = 0; ix < olds.length; ix++)
        s[ix] = olds[ix];
    }
    s[free++] = property;
    s[free++] = value;
  }

  public boolean isEmpty() {
    return free == 0;
  }
}