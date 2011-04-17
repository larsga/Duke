
package no.priv.garshol.duke;

import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;

/**
 * The actual deduplicating service.
 */
public class Deduplicator {
  private Database database;
  private Collection<Property> idproperties;

  public Deduplicator(Database database) {
    this.database = database;
    this.idproperties = database.getIdentityProperties();
  }

  /**
   * Processes a newly arrived batch of records. The records may have
   * been seen before.
   */
  public void process(Collection<Record> records)
    throws CorruptIndexException, IOException {
    // prepare
    for (Record record : records) {
      database.store(record);
      database.index(record);
    }

    database.commit();

    // then match
    for (Record record : records)
      match(record);
  }
  
  private void match(Record record) throws IOException {
    Set<Record> candidates = new HashSet(100);
    for (Property p : database.getLookupProperties())
      candidates.addAll(database.lookup(p, record.getValues(p.getName())));

    for (Record candidate : candidates) {
      if (isSameAs(record, candidate))
        continue;
      double prob = compare(record, candidate);
      if (prob > database.getThreshold())
        database.registerMatch(record, candidate, prob);
    }
  }

  public double compare(Record r1, Record r2) {
    double prob = 0.5;
    for (String propname : r1.getProperties()) {
      Property prop = database.getPropertyByName(propname);
      if (prop.isIdProperty())
        continue;

      double high = 0.0;
      for (String v1 : r1.getValues(propname)) {
        if (v1.equals(""))
          continue;
        
        for (String v2 : r2.getValues(propname)) {
          if (v2.equals(""))
            continue;
        
          try {
            high = Math.max(high, prop.compare(v1, v2));
          } catch (Exception e) {
            throw new RuntimeException("Comparison of values '" + v1 + "' and "+
                                       "'" + v2 + "' failed", e);
          }
        }
      }

      prob = Utils.computeBayes(prob, high);
    }
    return prob;
  }

  private boolean isSameAs(Record r1, Record r2) {
    for (Property idp : idproperties) {
      Collection<String> vs2 = r2.getValues(idp.getName());
      Collection<String> vs1 = r1.getValues(idp.getName());
      if (vs1 == null)
        continue;
      for (String v1 : vs1)
        if (vs2.contains(v1))
          return true;
    }
    return false;
  }
}