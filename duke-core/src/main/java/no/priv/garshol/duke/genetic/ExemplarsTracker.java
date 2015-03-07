
package no.priv.garshol.duke.genetic;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.matchers.AbstractMatchListener;

/**
 * A listener to decide which potential matches to ask the oracle
 * about.
 */
public class ExemplarsTracker extends AbstractMatchListener {
  // we cheat in this map, and map the pair onto itself, which is why
  // the pair object contains a counter (saves one object, and thus
  // some memory)
  private Map<Pair, Pair> exemplars;
  private Configuration config;
  private Comparator comparator;

  public ExemplarsTracker(Configuration config, Comparator comparator) {
    this.config = config;
    this.exemplars = new HashMap();
    this.comparator = comparator;
  }

  public synchronized void matches(Record r1, Record r2, double confidence) {
    Pair key = new Pair(getid(r1), getid(r2));
    Pair counter = exemplars.get(key);
    if (counter == null) {
      exemplars.put(key, key);
      counter = key;
    }
    counter.counter++;
  }

  public List<Pair> getExemplars() {
    List<Pair> sorted = new ArrayList(exemplars.size());
    sorted.addAll(exemplars.keySet());
    Collections.sort(sorted, comparator);
    return sorted;
  }

  private String getid(Record r) {
    for (String propname : r.getProperties())
      if (config.getPropertyByName(propname).isIdProperty())
        return r.getValue(propname);
    return null;
  }
}
