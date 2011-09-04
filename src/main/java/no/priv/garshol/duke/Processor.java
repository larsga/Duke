
package no.priv.garshol.duke;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;

import no.priv.garshol.duke.utils.Utils;

/**
 * The class that implements the actual deduplication and record
 * linkage logic.
 */
public class Processor {
  private Configuration config;
  private Database database;
  private Collection<MatchListener> listeners;

  /**
   * Creates a new processor, overwriting the existing Lucene index.
   */
  public Processor(Configuration config) 
    throws CorruptIndexException, IOException {
    this(config, true);
  }

  /**
   * Creates a new processor.
   * @param overwrite If true, make new Lucene index. If false, leave
   * existing data.
   */
  public Processor(Configuration config, boolean overwrite)
    throws CorruptIndexException, IOException {
    this.config = config;
    this.database = config.createDatabase(overwrite);
    this.listeners = new ArrayList();
  }

  /**
   * Adds a listener to be notified of processing events.
   */
  public void addMatchListener(MatchListener listener) {
    listeners.add(listener);
  }

  /**
   * Returns all registered listeners.
   */
  public Collection<MatchListener> getListeners() {
    return listeners;
  }

  /**
   * Returns the actual Lucene index being used.
   */
  public Database getDatabase() {
    return database;
  }

  /**
   * Reads all available records from the data sources and processes
   * them in batches.
   */
  public void deduplicate(Collection<DataSource> sources, Logger logger,
                          int batch_size) throws IOException {
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
          for (MatchListener listener : listeners)
            listener.batchReady(batch.size());
          deduplicate(batch);
          it2.batchProcessed();
          batch = new ArrayList();
        }
      }
      it2.close();
    }
      
    if (!batch.isEmpty()) {
      for (MatchListener listener : listeners)
        listener.batchReady(batch.size());
      deduplicate(batch);
    }

    for (MatchListener listener : listeners)
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
          for (MatchListener listener : listeners)
            listener.batchReady(batch_size);
        }
      }
      it2.close();
    }
    if (count % batch_size == 0) {
      for (MatchListener listener : listeners)
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
          for (MatchListener listener : listeners)
            listener.noMatchFor(record);
      }
      it2.close();
    }

    for (MatchListener listener : listeners)
      listener.endProcessing();
  }
  
  /**
   * Deduplicates a newly arrived batch of records. The records may
   * have been seen before.
   */
  public void deduplicate(Collection<Record> records) {
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

  // FIXME: it's possible that this method should be public
  private void match(Record record) throws IOException {
    startRecord(record);
    Set<Record> candidates = new HashSet(100);
    for (Property p : config.getLookupProperties())
      candidates.addAll(database.lookup(p, record.getValues(p.getName())));
    
    for (Record candidate : candidates) {
      if (isSameAs(record, candidate))
        continue;
      double prob = compare(record, candidate);
      if (prob > config.getThreshold())
        registerMatch(record, candidate, prob);
      else if (prob > config.getMaybeThreshold())
        registerMatchPerhaps(record, candidate, prob);
    }
    endRecord();
  }

  // FIXME: it's possible that this method should be public
  // package internal. used for record linkage only. returns true iff
  // a match was found.
  boolean matchRL(Record record) throws IOException {
    startRecord(record);
    Set<Record> candidates = new HashSet(100);
    for (Property p : config.getLookupProperties())
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
      if (max > config.getThreshold()) {
        registerMatch(record, best, max);
        found = true;
      } else if (max > config.getMaybeThreshold())
        registerMatchPerhaps(record, best, max);
    }

    endRecord();
    return found;
  }

  /**
   * Compares two records and returns the probability that they
   * represent the same real-world entity.
   */
  public double compare(Record r1, Record r2) {
    double prob = 0.5;
    for (String propname : r1.getProperties()) {
      Property prop = config.getPropertyByName(propname);
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

  /**
   * Commits all state to disk and frees up resources.
   */
  public void close() throws CorruptIndexException, IOException {
    database.close();
  }

  // ===== INTERNALS

  private boolean isSameAs(Record r1, Record r2) {
    for (Property idp : config.getProperties()) {
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

  /**
   * Notifies listeners that we started on this record.
   */
  private void startRecord(Record record) {
    for (MatchListener listener : listeners)
      listener.startRecord(record);
  }
  
  /**
   * Records the statement that the two records match.
   */
  private void registerMatch(Record r1, Record r2, double confidence) {
    for (MatchListener listener : listeners)
      listener.matches(r1, r2, confidence);
  }

  /**
   * Records the statement that the two records may match.
   */
  private void registerMatchPerhaps(Record r1, Record r2, double confidence) {
    for (MatchListener listener : listeners)
      listener.matchesPerhaps(r1, r2, confidence);
  }

  /**
   * Notifies listeners that we finished this record.
   */
  private void endRecord() {
    for (MatchListener listener : listeners)
      listener.endRecord();
  }
}