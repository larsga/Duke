
package no.priv.garshol.duke;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;

// FIXME: this class should merge with Database

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
   * Reads all available records from the data sources and processes
   * them in batches.
   */
  public void deduplicate(Collection<DataSource> sources, Logger logger,
                          int batch_size) throws IOException {
    Deduplicator dedup = new Deduplicator(database);
    Collection<Record> batch = new ArrayList();
    int count = 0;
    
    Iterator<DataSource> it = sources.iterator();
    while (it.hasNext()) {
      DataSource source = it.next();
      source.setLogger(logger);

      RecordIterator it2 = source.getRecords();
      while (it2.hasNext()) {
        Record record = it2.next();
        batch.add(record);
        count++;
        if (count % batch_size == 0) {
          for (MatchListener listener : database.getListeners())
            listener.batchReady(batch.size());
          process(batch);
          it2.batchProcessed();
          batch = new ArrayList();
        }
      }
      it2.close();
    }
      
    if (!batch.isEmpty()) {
      for (MatchListener listener : database.getListeners())
        listener.batchReady(batch.size());
      process(batch);
    }

    for (MatchListener listener : database.getListeners())
      listener.endProcessing();
  }

  // FIXME: what about the general case, where there are more than 2 groups?
  /**
   * Does record linkage across the two groups, but does not link
   * records within each group.
   */
  public void link(Collection<DataSource> sources1,
                   Collection<DataSource> sources2,
                   Logger logger, int batch_size) throws IOException {
    // first, index up group 1
    int count = 0;
    for (DataSource source : sources1) {
      source.setLogger(logger);

      RecordIterator it2 = source.getRecords();
      while (it2.hasNext()) {
        Record record = it2.next();
        database.index(record);
        count++;
        if (count % batch_size == 0) {
          for (MatchListener listener : database.getListeners())
            listener.batchReady(batch_size);
        }
      }
      it2.close();
    }
    if (count % batch_size == 0) {
      for (MatchListener listener : database.getListeners())
        listener.batchReady(count % batch_size);
    }
    database.commit();

    // second, traverse group 2 to look for matches with group 1
    for (DataSource source : sources2) {
      source.setLogger(logger);

      RecordIterator it2 = source.getRecords();
      while (it2.hasNext()) {
        Record record = it2.next();
        boolean found = matchRL(record);
        if (!found)
          for (MatchListener listener : database.getListeners())
            listener.noMatchFor(record);
      }
      it2.close();
    }

    for (MatchListener listener : database.getListeners())
      listener.endProcessing();
  }
  
  /**
   * Processes a newly arrived batch of records. The records may have
   * been seen before.
   */
  public void process(Collection<Record> records) {
    try {
      // prepare
      for (Record record : records)
        database.index(record);

      database.commit();

      // then match
      for (Record record : records)
        match(record);
    } catch (CorruptIndexException e) {
      throw new DukeException(e);
    } catch (IOException e) {
      throw new DukeException(e);
    }
  }
  
  private void match(Record record) throws IOException {
    database.startRecord(record);
    Set<Record> candidates = new HashSet(100);
    for (Property p : database.getLookupProperties())
      candidates.addAll(database.lookup(p, record.getValues(p.getName())));
    
    for (Record candidate : candidates) {
      if (isSameAs(record, candidate))
        continue;
      double prob = compare(record, candidate);
      if (prob > database.getThreshold())
        database.registerMatch(record, candidate, prob);
      else if (prob > database.getMaybeThreshold())
        database.registerMatchPerhaps(record, candidate, prob);
    }
    database.endRecord();
  }

  // package internal. used for record linkage only. returns true iff
  // a match was found.
  boolean matchRL(Record record) throws IOException {
    database.startRecord(record);
    Set<Record> candidates = new HashSet(100);
    for (Property p : database.getLookupProperties())
      candidates.addAll(database.lookup(p, record.getValues(p.getName())));

    double max = 0.0;
    Record best = null;
    for (Record candidate : candidates) {
      if (isSameAs(record, candidate))
        continue;
      
      double prob = compare(record, candidate);
      if (prob > max) {
        max = prob;
        best = candidate;
      }
    }

    boolean found = false;
    if (best != null) {
      if (max > database.getThreshold()) {
        database.registerMatch(record, best, max);
        found = true;
      } else if (max > database.getMaybeThreshold())
        database.registerMatchPerhaps(record, best, max);
    }

    database.endRecord();
    return found;
  }
  
  public double compare(Record r1, Record r2) {
    double prob = 0.5;
    for (String propname : r1.getProperties()) {
      Property prop = database.getPropertyByName(propname);
      if (prop.isIdProperty())
        continue;

      Collection<String> vs1 = r1.getValues(propname);
      Collection<String> vs2 = r2.getValues(propname);
      if (vs1.isEmpty() || vs2.isEmpty())
        continue; // no values to compare, so skip
      
      double high = 0.0;
      for (String v1 : vs1) {
        if (v1.equals("")) // FIXME: these values shouldn't be here at all
          continue;
        
        for (String v2 : vs2) {
          if (v2.equals("")) // FIXME: these values shouldn't be here at all
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