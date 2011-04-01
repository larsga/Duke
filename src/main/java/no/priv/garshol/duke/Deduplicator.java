
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

  public Deduplicator(Database database) {
    this.database = database;
  }

  /**
   * Processes a newly arrived record. The record may have been seen before.
   */
  public void process(Record record) throws CorruptIndexException, IOException {
    database.store(record);
    record.clean(database);

    for (String id : record.getIdentities()) {
      database.unindex(id);
      database.removeEqualities(id);
    }
    
    match(record);

    database.index(record);
  }

  private void match(Record record) throws IOException {
    Set<Record> candidates = new HashSet(100);
    for (Property p : database.getLookupProperties())
      candidates.addAll(database.lookup(p, record.getValues(p.getName())));

    //System.out.println(candidates.size() + " " + database.docs_since_opt);

    for (Record candidate : candidates) {
      double prob = compare(record, candidate);
      if (prob > database.getThreshold())
        database.registerMatch(record, candidate, prob);
    }
  }

  private double compare(Record r1, Record r2) {
    double prob = 0.5;
    for (String propname : r1.getProperties()) {
      Property prop = database.getPropertyByName(propname);
      if (prop.isIdProperty())
        continue;

      double high = 0.0;
      for (String v1 : r1.getValues(propname))
        for (String v2 : r2.getValues(propname))
          high = Math.max(high, prop.compare(v1, v2));

      prob = Utils.computeBayes(prob, high);
    }
    return prob;
  }
}