
package no.priv.garshol.duke;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;

import no.priv.garshol.duke.utils.Utils;
import no.priv.garshol.duke.matchers.MatchListener;
import no.priv.garshol.duke.matchers.PrintMatchListener;
import no.priv.garshol.duke.matchers.AbstractMatchListener;

/**
 * The class that implements the actual deduplication and record
 * linkage logic.
 */
public class Processor {
  private Configuration config;
  protected Database database;
  private Collection<MatchListener> listeners;
  private Logger logger;
  private List<Property> proporder;
  private double[] accprob;
  private final static int DEFAULT_BATCH_SIZE = 40000;

  private MatchListener passthrough;
  private MatchListener choosebest;

  /**
   * Creates a new processor, overwriting the existing Lucene index.
   */
  public Processor(Configuration config) throws IOException {
    this(config, true);
  }

  /**
   * Creates a new processor.
   * @param overwrite If true, make new Lucene index. If false, leave
   * existing data.
   */
  public Processor(Configuration config, boolean overwrite) throws IOException {
    this(config, config.createDatabase(overwrite));
  }

  /**
   * Creates a new processor, bound to the given database.
   */
  public Processor(Configuration config, Database database) throws IOException {
    this.config = config;
    this.database = database;
    this.listeners = new ArrayList<MatchListener>();
    this.logger = new DummyLogger();

    this.passthrough = new PassThroughFilter();
    this.choosebest = new ChooseBestFilter();

    // precomputing for later optimizations
    this.proporder = new ArrayList();
    for (Property p : config.getProperties())
      if (!p.isIdProperty())
        proporder.add(p);
    Collections.sort(proporder, new PropertyComparator());

    // still precomputing
    double prob = 0.5;
    accprob = new double[proporder.size()];
    for (int ix = proporder.size() - 1; ix >= 0; ix--) {
      prob = Utils.computeBayes(prob, proporder.get(ix).getHighProbability());
      accprob[ix] = prob;
    }
  }
  
  /**
   * Sets the logger to report to.
   */
  public void setLogger(Logger logger) {
    this.logger = logger;
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
   * them in batches, notifying the listeners throughout.
   */
  public void deduplicate() throws IOException {
    deduplicate(config.getDataSources(), DEFAULT_BATCH_SIZE);
  }
  
  /**
   * Reads all available records from the data sources and processes
   * them in batches, notifying the listeners throughout.
   */
  public void deduplicate(Collection<DataSource> sources, int batch_size)
    throws IOException {
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
  
  /**
   * Deduplicates a newly arrived batch of records. The records may
   * have been seen before.
   */
  public void deduplicate(Collection<Record> records) {
    logger.info("Deduplicating batch of " + records.size() + " records");
    try {
      // prepare
      for (Record record : records)
        database.index(record);

      database.commit();

      // then match
      for (Record record : records)
        match(record, passthrough);

      for (MatchListener listener : listeners)
        listener.batchDone();
    } catch (CorruptIndexException e) {
      throw new DukeException(e);
    } catch (IOException e) {
      throw new DukeException(e);
    }
  }

  /**
   * Does record linkage across the two groups, but does not link
   * records within each group.
   */
  public void link() throws IOException {
    link(config.getDataSources(1), config.getDataSources(2),
         DEFAULT_BATCH_SIZE);
  }

  // FIXME: what about the general case, where there are more than 2 groups?
  /**
   * Does record linkage across the two groups, but does not link
   * records within each group.
   */
  public void link(Collection<DataSource> sources1,
                   Collection<DataSource> sources2,
                   int batch_size) throws IOException {
    // first, index up group 1
    index(sources1, batch_size);

    // second, traverse group 2 to look for matches with group 1
    linkRecords(sources2, choosebest);
  }

  /**
   * Retrieve new records from data sources, and match them to
   * previously indexed records. This method does <em>not</em> index
   * the new records. With this method, <em>all</em> matches above
   * threshold are passed on.
   * @since 0.4
   */
  public void linkRecords(Collection<DataSource> sources)
    throws IOException {
    linkRecords(sources, passthrough);
  }

  /**
   * Retrieve new records from data sources, and match them to
   * previously indexed records. This method does <em>not</em> index
   * the new records.
   * @param matchall If true, all matching records are accepted. If false,
   *                 only the single best match is accepted.
   * @since 0.5
   */
  public void linkRecords(Collection<DataSource> sources, boolean matchall)
    throws IOException {
    linkRecords(sources, matchall ? passthrough : choosebest);
  }
  
  /**
   * Retrieve new records from data sources, and match them to
   * previously indexed records. This method does <em>not</em> index
   * the new records.
   */
  private void linkRecords(Collection<DataSource> sources,
                           MatchListener filter)
    throws IOException {
    for (DataSource source : sources) {
      source.setLogger(logger);

      RecordIterator it = source.getRecords();
      while (it.hasNext()) {
        Record record = it.next();
        match(record, filter);
      }
      it.close();
    }

    for (MatchListener listener : listeners)
      listener.endProcessing();
  }

  /**
   * Index all new records from the given data sources. This method
   * does <em>not</em> do any matching.
   * @since 0.4
   */
  public void index(Collection<DataSource> sources, int batch_size)
    throws IOException {
    int count = 0;
    for (DataSource source : sources) {
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
  }

  private void match(Record record, MatchListener filter) throws IOException {
    Set<Record> candidates = new HashSet(100);
    for (Property p : config.getLookupProperties())
      candidates.addAll(database.lookup(p, record.getValues(p.getName())));

    if (logger.isDebugEnabled())
      logger.debug("Matching record " + PrintMatchListener.toString(record) +
                   " found " + candidates.size() + " candidates");

    compareCandidates(record, candidates, filter);
  }

  protected void compareCandidates(Record record, Collection<Record> candidates,
                                   MatchListener filter) {
    filter.startRecord(record);
    for (Record candidate : candidates) {
      if (isSameAs(record, candidate))
        continue;

      double prob = compare(record, candidate);
      if (prob > config.getThreshold())
        filter.matches(record, candidate, prob);
      else if (prob > config.getMaybeThreshold())
        filter.matchesPerhaps(record, candidate, prob);
    }
    filter.endRecord();
  }

  /**
   * Compares two records and returns the probability that they
   * represent the same real-world entity.
   */
  public double compare(Record r1, Record r2) {
    double prob = 0.5;
    for (String propname : r1.getProperties()) {
      Property prop = config.getPropertyByName(propname);
      if (prop.isIdProperty() || prop.getHighProbability() == 0.0)
        // some people set high probability to zero, which means these
        // properties will prevent any matches from occurring at all if
        // we try to use them. so we skip these.
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
            double p = prop.compare(v1, v2);
            high = Math.max(high, p);
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
  public void close() throws IOException {
    database.close();
  }

  // ===== INTERNALS

  private boolean isSameAs(Record r1, Record r2) {
    for (Property idp : config.getIdentityProperties()) {
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
  private void registerStartRecord(Record record) {
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
   * Notifies listeners that we found no matches for this record.
   */
  private void registerNoMatchFor(Record current) {
    for (MatchListener listener : listeners)
      listener.noMatchFor(current);
  }
  
  /**
   * Notifies listeners that we finished this record.
   */
  private void registerEndRecord() {
    for (MatchListener listener : listeners)
      listener.endRecord();
  }

  /**
   * Sorts properties so that the properties with the lowest low
   * probabilities come first.
   */
  static class PropertyComparator implements Comparator<Property> {
    public int compare(Property p1, Property p2) {
      double diff = p1.getLowProbability() - p2.getLowProbability();
      if (diff < 0)
        return -1;
      else if (diff > 0)
        return 1;
      else
        return 0;
    }
  }

  // ===== FILTERS
  // these internal listeners are used to implement different record
  // matching strategies. the first is used for deduplication, where
  // we simply want all matches above the thresholds. the second is
  // used for record linkage, to implement a simple greedy matching
  // algorithm where we choose the best alternative above the
  // threshold for each record.

  // other, more advanced possibilities exist for record linkage, but
  // they are not implemented yet. see the links below for more
  // information.
  
  // http://code.google.com/p/duke/issues/detail?id=55
  // http://research.microsoft.com/pubs/153478/msr-report-1to1.pdf
  
  class PassThroughFilter extends AbstractMatchListener {

    public void startRecord(Record r) {
      registerStartRecord(r);
    }
    
    public void matches(Record r1, Record r2, double confidence) {
      registerMatch(r1, r2, confidence);
    }

    public void matchesPerhaps(Record r1, Record r2, double confidence) {
      registerMatchPerhaps(r1, r2, confidence);
    }

    public void endRecord() {
      registerEndRecord();
    }
  }

  class ChooseBestFilter extends AbstractMatchListener {
    private Record current;
    private Record best;
    private double max;
    
    public void startRecord(Record r) {
      registerStartRecord(r);
      max = 0;
      best = null;
      current = r;
    }
    
    public void matches(Record r1, Record r2, double confidence) {
      if (confidence > max) {
        max = confidence;
        best = r2;
      }
    }

    public void matchesPerhaps(Record r1, Record r2, double confidence) {
      matches(r1, r2, confidence);
    }

    public void endRecord() {
      if (max > config.getThreshold())
        registerMatch(current, best, max);
      else if (config.getMaybeThreshold() != 0.0 &&
               max > config.getMaybeThreshold())
        registerMatchPerhaps(current, best, max);
      else
        registerNoMatchFor(current);
      registerEndRecord();
    }
  }
}