
package no.priv.garshol.duke;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import no.priv.garshol.duke.utils.StringUtils;

/**
 * A database that uses a key-value store to index and find records.
 * Currently an experimental proof of concept to see if this approach
 * really can be faster that Lucene.
 */
public class KeyValueDatabase implements Database {
  private Configuration config;
  private KeyValueStore store;
  private int max_search_hits;
  private float min_relevance;
  
  public KeyValueDatabase(Configuration config,
                          DatabaseProperties dbprops) {
    this.config = config;
    this.max_search_hits = dbprops.getMaxSearchHits();
    this.min_relevance = dbprops.getMinRelevance();
    this.store = new InMemoryKeyValueStore();
  }

  /**
   * Returns true iff the database is held entirely in memory, and
   * thus is not persistent.
   */
  public boolean isInMemory() {
    return store.isInMemory();
  }

  /**
   * Add the record to the index.
   */
  public void index(Record record) {
    // FIXME: check if record is already indexed

    // allocate an ID for this record
    long id = store.makeNewRecordId();
    store.registerRecord(id, record);
    
    // go through ID properties and register them
    for (Property p : config.getIdentityProperties()) {
      Collection<String> values = record.getValues(p.getName());
      if (values == null)
        continue;
      
      for (String extid : values)
        store.registerId(id, extid);
    }

    // go through lookup properties and register those
    for (Property p : config.getLookupProperties()) {
      String propname = p.getName();
      Collection<String> values = record.getValues(propname);
      if (values == null)
        continue;

      for (String value : values) {
        String[] tokens = StringUtils.split(value);
        for (int ix = 0; ix < tokens.length; ix++)
          store.registerToken(id, propname, tokens[ix]);
      }
    }
  }

  /**
   * Look up record by identity.
   */
  public Record findRecordById(String id) {
    return store.findRecordById(id);
  }

  /**
   * Look up potentially matching records.
   */
  public Collection<Record> findCandidateMatches(Record record) {
    // the collection of candidates
    Map<Long, Score> candidates = new HashMap();
    
    // do lookup on all tokens from all lookup properties
    for (Property p : config.getLookupProperties()) {
      String propname = p.getName();
      Collection<String> values = record.getValues(propname);
      if (values == null)
        continue;

      for (String value : values) {
        String[] tokens = StringUtils.split(value);
        for (int ix = 0; ix < tokens.length; ix++) {
          long[] ids = store.lookupToken(propname, tokens[ix]);
          double score = 1.0 / (double) ids.length; // IDF (assume TF = 1)
          for (int ix2 = 0; ix < ids.length; ix++) {
            Score s = candidates.get(ids[ix2]);
            if (s == null) {
              s = new Score(ids[ix2]);
              candidates.put(ids[ix2], s);
            }
            s.score += score;
          }
        }
      }
    }

    // if the cutoff properties are not set we can stop right here
    if (max_search_hits == 0 && min_relevance == 0.0) {
      Collection<Record> cands = new ArrayList(candidates.size());
      for (Long id : candidates.keySet())
        cands.add(store.findRecordById(id));
      return cands;
    }
    
    // flatten candidates into an array, prior to sorting etc
    double max_score = 0.0;
    int ix = 0;
    Score[] scores = new Score[candidates.size()];
    for (Score s : candidates.values()) {
      scores[ix++] = s;
      if (s.score > max_score)
        max_score = s.score;
    }

    // allow map to be GC-ed
    candidates = null;

    // remove all candidates with scores below min_relevance
    int nextfree = 0;
    if (min_relevance != 0.0) {
      for (ix = 0; ix < scores.length; ix++) {
        if (scores[ix].score / max_score >= min_relevance)
          scores[nextfree++] = scores[ix];
      }
      if (nextfree < scores.length)
        scores[nextfree] = null; // mark the end of the values
    } else
      nextfree = scores.length; // simplifies following code

    // remove all candidates except the best max_search_hits
    if (max_search_hits != 0 && max_search_hits < nextfree) {
      Arrays.sort(scores, 0, nextfree);
      scores[max_search_hits] = null; // mark the end again
      nextfree = max_search_hits;
    }

    // now we can retrieve the candidates and return
    Collection<Record> records = new ArrayList(nextfree);
    for (ix = 0; ix < nextfree; ix++)
      records.add(store.findRecordById(scores[ix].id));
    return records;
  }

  /**
   * Flushes all changes to disk. For in-memory databases this is a
   * no-op.
   */
  public void commit() {
    store.commit();
  }
  
  /**
   * Stores state to disk and closes all open resources.
   */
  public void close() {
    store.close();
  }

  public String toString() {
    return "KeyValueDatabase(" + store + ")";
  }

  static class Score {
    public long id;
    public double score;

    public Score(long id) {
      this.id = id;
    }
  }
}
