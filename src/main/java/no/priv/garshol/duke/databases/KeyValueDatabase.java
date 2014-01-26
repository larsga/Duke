
package no.priv.garshol.duke.databases;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.utils.StringUtils;

/**
 * A database that uses a key-value store to index and find records.
 * Faster than Lucene, but relevance ranking is not as good, and has
 * no fuzzy or geospatial support.
 * @since 1.0
 */
public class KeyValueDatabase implements Database {
  private Configuration config;
  private KeyValueStore store;
  private int max_search_hits;
  private float min_relevance;
  private static final boolean DEBUG = false;

  // we'll never gather more candidates than CF1 * max_search_hits
  private static final int CUTOFF_FACTOR_1 = 20;

  // buckets that have more elements than candidates.size * CF2 are ignored
  private static final int CUTOFF_FACTOR_2 = 50;
  
  public KeyValueDatabase() {
    this.store = new InMemoryKeyValueStore();
    this.max_search_hits = 1000000;
  }

  public void setConfiguration(Configuration config) {
    this.config = config;
  }

  public void setOverwrite(boolean overwrite) {
  }

  public void setMaxSearchHits(int max_search_hits) {
    this.max_search_hits = max_search_hits;
  }

  public void setMinRelevance(float min_relevance) {
    this.min_relevance = min_relevance;
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
    for (Property p : config.getIdentityProperties())
      for (String extid : record.getValues(p.getName()))
        store.registerId(id, extid);

    // go through lookup properties and register those
    for (Property p : config.getLookupProperties()) {
      String propname = p.getName();
      for (String value : record.getValues(propname)) {
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
    if (DEBUG)
      System.out.println("---------------------------------------------------------------------------");
    
    // do lookup on all tokens from all lookup properties
    // (we only identify the buckets for now. later we decide how to process
    // them)
    List<Bucket> buckets = lookup(record);
    
    // preprocess the list of buckets
    Collections.sort(buckets);
    double score_sum = 0.0;
    for (Bucket b : buckets)
      score_sum += b.getScore();
      
    double score_so_far = 0.0;
    int threshold = buckets.size() - 1;
    for (; (score_so_far / score_sum) < min_relevance; threshold--) {
      score_so_far += buckets.get(threshold).getScore();
      if (DEBUG)
        System.out.println("score_so_far: " + (score_so_far/score_sum) + " (" +
                           threshold + ")");
    }
    // bucket.get(threshold) made us go over the limit, so we need to step
    // one back
    threshold++;
    if (DEBUG)
      System.out.println("Threshold: " + threshold);
    
    // the collection of candidates
    Map<Long, Score> candidates = new HashMap();

    // go through the buckets that we're going to collect candidates from
    int next_bucket = collectCandidates(candidates, buckets, threshold);

    // there might still be some buckets left below the threshold. for
    // these we go through the existing candidates and check if we can
    // find them in the buckets.
    bumpScores(candidates, buckets, next_bucket);

    if (DEBUG)
      System.out.println("candidates: " + candidates.size());
    
    // if the cutoff properties are not set we can stop right here
    // FIXME: it's possible to make this a lot cleaner
    if (max_search_hits > candidates.size() && min_relevance == 0.0) {
      Collection<Record> cands = new ArrayList(candidates.size());
      for (Long id : candidates.keySet())
        cands.add(store.findRecordById(id));
      if (DEBUG)
        System.out.println("final: " + cands.size());
      return cands;
    }
    
    // flatten candidates into an array, prior to sorting etc
    int ix = 0;
    Score[] scores = new Score[candidates.size()];
    double max_score = 0.0;
    for (Score s : candidates.values()) {
      scores[ix++] = s;
      if (s.score > max_score)
        max_score = s.score;
      if (DEBUG && false)
        System.out.println("" + s.id + ": " + s.score);
    }

    // allow map to be GC-ed
    candidates = null;

    // filter candidates with min_relevance and max_search_hits. do
    // this by turning the scores[] array into a priority queue (on
    // .score), then retrieving the best candidates. (gives a big
    // performance improvement over sorting the array.)
    PriorityQueue pq = new PriorityQueue(scores);
    int count = Math.min(scores.length, max_search_hits);
    Collection<Record> records = new ArrayList(count);
    for (ix = 0; ix < count; ix++) {
      Score s = pq.next();
      if (s.score >= min_relevance)
        records.add(store.findRecordById(s.id));
    }

    if (DEBUG)
      System.out.println("final: " + records.size());
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
    return "KeyValueDatabase(" + store + "), max_search_hits=" +
      max_search_hits + ", min_relevance=" + min_relevance;
  }

  /**
   * Goes through the buckets from ix and out, checking for each
   * candidate if it's in one of the buckets, and if so, increasing
   * its score accordingly. No new candidates are added.
   */ 
  private void bumpScores(Map<Long, Score> candidates,
                          List<Bucket> buckets,
                          int ix) {
    for (; ix < buckets.size(); ix++) {
      Bucket b = buckets.get(ix);
      if (b.nextfree > CUTOFF_FACTOR_2 * candidates.size())
        return;
      double score = b.getScore();
      for (Score s : candidates.values())
        if (b.contains(s.id))
          s.score += score;
    }
  }  
  
  /**
   * Goes through the first buckets, picking out candidate records and
   * tallying up their scores.
   * @return the index of the first bucket we did not process
   */
  private int collectCandidates(Map<Long, Score> candidates,
                                List<Bucket> buckets,
                                int threshold) {
    int ix;
    for (ix = 0; ix < threshold &&
           candidates.size() < (CUTOFF_FACTOR_1 * max_search_hits); ix++) {
      Bucket b = buckets.get(ix);
      long[] ids = b.records;
      double score = b.getScore();
      
      for (int ix2 = 0; ix2 < b.nextfree; ix2++) {
        Score s = candidates.get(ids[ix2]);
        if (s == null) {
          s = new Score(ids[ix2]);
          candidates.put(ids[ix2], s);
        }
        s.score += score;
      }
      if (DEBUG)
        System.out.println("Bucket " + b.nextfree + " -> " + candidates.size());
    }
    return ix;
  }
  
  /**
   * Tokenizes lookup fields and returns all matching buckets in the
   * index.
   */
  private List<Bucket> lookup(Record record) {
    List<Bucket> buckets = new ArrayList();
    for (Property p : config.getLookupProperties()) {
      String propname = p.getName();
      Collection<String> values = record.getValues(propname);
      if (values == null)
        continue;

      for (String value : values) {
        String[] tokens = StringUtils.split(value);
        for (int ix = 0; ix < tokens.length; ix++) {
          Bucket b = store.lookupToken(propname, tokens[ix]);
          if (b == null || b.records == null)
            continue;
          long[] ids = b.records;
          if (DEBUG)
            System.out.println(propname + ", " + tokens[ix] + ": " + b.nextfree + " (" + b.getScore() + ")");
          buckets.add(b);
        }
      }
    }

    return buckets;
  }

  // public so that we can test the priority queue
  public static class Score implements Comparable<Score> {
    public long id;
    public double score;

    public Score(long id) {
      this.id = id;
    }

    public int compareTo(Score other) {
      if (other.score < score)
        return -1;
      else if (other.score > score)
        return 1;
      else
        return 0;
    }
  }

  // public so that we can test it
  public static class PriorityQueue {
    private Score[] scores;
    private int size;

    public PriorityQueue(Score[] scores) {
      this.scores = scores;
      this.size = scores.length; // heap is always full to begin with
      build_heap();
    }

    /**
     * Turns the random array into a heap.
     */
    private void build_heap() {
      for (int ix = (size / 2); ix >= 0; ix--)
        heapify(ix);
    }

    /**
     * Assuming binary trees rooted at left(ix) and right(ix) are
     * already heaped, but scores[ix] may not be heaped, rebalance so
     * that scores[ix] winds up in the right place, and subtree rooted
     * at ix is correctly heaped.
     */
    private void heapify(int ix) {
      int left = (ix * 2) + 1;
      if (left >= size)
        return; // ix is a leaf, and there's nothing to be done
      
      int right = left + 1;
      int largest = ix;
      if (scores[left].score > scores[ix].score)
        largest = left;

      if (right < size && scores[right].score > scores[largest].score)
        largest = right;

      if (largest != ix) {
        Score tmp = scores[largest];
        scores[largest] = scores[ix];
        scores[ix] = tmp;
        heapify(largest);
      }
    }

    public Score next() {
      Score next = scores[0];
      size--;
      if (size >= 0) {
        scores[0] = scores[size];
        scores[size] = null;
        heapify(0);
      }
      return next;
    }
  }
}
