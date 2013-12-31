
package no.priv.garshol.duke;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.Writer;
import java.io.PrintWriter;

import no.priv.garshol.duke.matchers.MatchListener;
import no.priv.garshol.duke.matchers.PrintMatchListener;
import no.priv.garshol.duke.matchers.AbstractMatchListener;
import no.priv.garshol.duke.utils.Utils;

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
  private int threads;
  private final static int DEFAULT_BATCH_SIZE = 40000;

  // performance statistics
  private long comparisons; // number of records compared
  private long srcread; // ms spent reading from data sources
  private long indexing; // ms spent indexing records
  private long searching; // ms spent searching for records
  private long comparing; // ms spent comparing records
  private long callbacks; // ms spent in callbacks
  private Profiler profiler;

  /**
   * Creates a new processor, overwriting the existing Lucene index.
   */
  public Processor(Configuration config) {
    this(config, true);
  }

  /**
   * Creates a new processor.
   * @param overwrite If true, make new Lucene index. If false, leave
   * existing data.
   */
  public Processor(Configuration config, boolean overwrite) {
    this(config, config.getDatabase(overwrite));
  }

  /**
   * Creates a new processor, bound to the given database.
   */
  public Processor(Configuration config, Database database) {
    this.config = config;
    this.database = database;
    // using this List implementation so that listeners can be removed
    // while Duke is running (see issue 117)
    this.listeners = new CopyOnWriteArrayList<MatchListener>();
    this.logger = new DummyLogger();
    this.threads = 1;

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
   * Sets the number of threads to use for processing. The default is
   * 1.
   */
  public void setThreads(int threads) {
    this.threads = threads;
  }

  /**
   * Returns the number of threads.
   */
  public int getThreads() {
    return threads;
  }

  /**
   * Adds a listener to be notified of processing events.
   */
  public void addMatchListener(MatchListener listener) {
    listeners.add(listener);
  }
  
  /**
   * Removes a listener from being notified of the processing events.
   * @since 1.1
   */
  public boolean removeMatchListener(MatchListener listener) {
    if (listener != null)
      return listeners.remove(listener);
    return true;
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
   * Used to turn performance profiling on and off.
   * @since 1.1
   */
  public void setPerformanceProfiling(boolean profile) {
    if (profile) {
      if (profiler != null)
        return; // we're already profiling

      this.profiler = new Profiler();
      addMatchListener(profiler);

    } else {
      // turn off profiling
      if (profiler == null)
        return; // we're not profiling, so nothing to do

      removeMatchListener(profiler);
      profiler = null;
    }
  }

  /**
   * Returns the performance profiler, if any.
   * @since 1.1
   */
  public Profiler getProfiler() {
    return profiler;
  }
  
  /**
   * Reads all available records from the data sources and processes
   * them in batches, notifying the listeners throughout.
   */
  public void deduplicate() {
    deduplicate(config.getDataSources(), DEFAULT_BATCH_SIZE);
  }

  /**
   * Reads all available records from the data sources and processes
   * them in batches, notifying the listeners throughout.
   */
  public void deduplicate(int batch_size) {
    deduplicate(config.getDataSources(), batch_size);
  }
  
  /**
   * Reads all available records from the data sources and processes
   * them in batches, notifying the listeners throughout.
   */
  public void deduplicate(Collection<DataSource> sources, int batch_size) {
    Collection<Record> batch = new ArrayList();
    int count = 0;
    startProcessing();
    
    Iterator<DataSource> it = sources.iterator();
    while (it.hasNext()) {
      DataSource source = it.next();
      source.setLogger(logger);

      RecordIterator it2 = source.getRecords();
      try {
        long start = System.currentTimeMillis();
        while (it2.hasNext()) {
          Record record = it2.next();
          batch.add(record);
          count++;
          if (count % batch_size == 0) {
            srcread += (System.currentTimeMillis() - start);
            deduplicate(batch);
            it2.batchProcessed();
            batch = new ArrayList();
            start = System.currentTimeMillis();
          }
        }
      } finally {
        it2.close();
      }
    }
      
    if (!batch.isEmpty())
      deduplicate(batch);

    endProcessing();
  }
  
  /**
   * Deduplicates a newly arrived batch of records. The records may
   * have been seen before.
   */
  public void deduplicate(Collection<Record> records) {
    logger.info("Deduplicating batch of " + records.size() + " records");
    batchReady(records.size());
	  
    // prepare
    long start = System.currentTimeMillis();
    for (Record record : records)
      database.index(record);
	
    database.commit();
    indexing += System.currentTimeMillis() - start;
	  
    // then match
    match(records, true);

    batchDone();
  }

  private void match(Collection<Record> records, boolean matchall) {
    if (threads == 1)
      for (Record record : records)
        match(record, matchall);
    else
      threadedmatch(records, matchall);
  }

  private void threadedmatch(Collection<Record> records, boolean matchall) {
    // split batch into n smaller batches
    MatchThread[] threads = new MatchThread[this.threads];
    for (int ix = 0; ix < threads.length; ix++)
      threads[ix] = new MatchThread(ix, records.size() / threads.length,
                                    matchall);
    int ix = 0;
    for (Record record : records)      
      threads[ix++ % threads.length].addRecord(record);
      
    // kick off threads
    for (ix = 0; ix < threads.length; ix++)
      threads[ix].start();

    // wait for threads to finish
    try {
      for (ix = 0; ix < threads.length; ix++)
        threads[ix].join();
    } catch (InterruptedException e) {
      // argh
    }
  }

  /**
   * Does record linkage across the two groups, but does not link
   * records within each group.
   */
  public void link() {
    link(config.getDataSources(1), config.getDataSources(2),
         DEFAULT_BATCH_SIZE);
  }

  // FIXME: what about the general case, where there are more than 2 groups?
  /**
   * Does record linkage across the two groups, but does not link
   * records within each group. With this method, <em>all</em> matches
   * above threshold are passed on.
   */
  public void link(Collection<DataSource> sources1,
                   Collection<DataSource> sources2,
                   int batch_size) {
    link(sources1, sources2, true, batch_size);
  }

  /**
   * Does record linkage across the two groups, but does not link
   * records within each group.
   * @param matchall If true, all matching records are accepted. If false,
   *                 only the single best match for each record is accepted.
   * @param batch_size The batch size to use.
   * @since 1.1
   */
  public void link(Collection<DataSource> sources1,
                   Collection<DataSource> sources2,
                   boolean matchall,
                   int batch_size) {
    startProcessing();
    
    // first, index up group 1
    index(sources1, batch_size);

    // second, traverse group 2 to look for matches with group 1
    linkRecords(sources2, matchall, batch_size);
  }
  
  /**
   * Retrieve new records from data sources, and match them to
   * previously indexed records. This method does <em>not</em> index
   * the new records. With this method, <em>all</em> matches above
   * threshold are passed on.
   * @since 0.4
   */
  public void linkRecords(Collection<DataSource> sources) {
    linkRecords(sources, true);
  }

  /**
   * Retrieve new records from data sources, and match them to
   * previously indexed records. This method does <em>not</em> index
   * the new records.
   * @param matchall If true, all matching records are accepted. If false,
   *                 only the single best match for each record is accepted.
   * @since 0.5
   */
  public void linkRecords(Collection<DataSource> sources, boolean matchall) {
    linkRecords(sources, matchall, DEFAULT_BATCH_SIZE);
  }

  /**
   * Retrieve new records from data sources, and match them to
   * previously indexed records. This method does <em>not</em> index
   * the new records.
   * @param matchall If true, all matching records are accepted. If false,
   *                 only the single best match for each record is accepted.
   * @param batch_size The batch size to use.
   * @since 1.0
   */
  public void linkRecords(Collection<DataSource> sources, boolean matchall,
                          int batch_size) {
    for (DataSource source : sources) {
      source.setLogger(logger);

      Collection<Record> batch = new ArrayList(batch_size);
      RecordIterator it = source.getRecords();
      while (it.hasNext()) {
        batch.add(it.next());
        if (batch.size() == batch_size) {
          linkBatch(batch, matchall);
          batch.clear();
        }
      }
      it.close();

      if (!batch.isEmpty())
        linkBatch(batch, matchall);
    }

    endProcessing();
  }

  private void linkBatch(Collection<Record> batch, boolean matchall) {
    batchReady(batch.size());
    match(batch, matchall);
    batchDone();
  }
  
  /**
   * Index all new records from the given data sources. This method
   * does <em>not</em> do any matching.
   * @since 0.4
   */
  public void index(Collection<DataSource> sources, int batch_size) {
    int count = 0;
    for (DataSource source : sources) {
      source.setLogger(logger);

      RecordIterator it2 = source.getRecords();
      while (it2.hasNext()) {
        Record record = it2.next();
        database.index(record);
        count++;
        if (count % batch_size == 0)
          batchReady(batch_size);
      }
      it2.close();
    }
    if (count % batch_size == 0)
      batchReady(count % batch_size);
    database.commit();
  }

  /**
   * Returns the number of records that have been compared.
   */
  public long getComparisonCount() {
    return comparisons;
  }

  private void match(Record record, boolean matchall) {
    long start = System.currentTimeMillis();
    Collection<Record> candidates = database.findCandidateMatches(record);
    searching += System.currentTimeMillis() - start;
    if (logger.isDebugEnabled())
      logger.debug("Matching record " +
                   PrintMatchListener.toString(record, config.getProperties()) +
                   " found " + candidates.size() + " candidates");

    start = System.currentTimeMillis();
    if (matchall)
      compareCandidatesSimple(record, candidates);
    else
      compareCandidatesBest(record, candidates);
    comparing += System.currentTimeMillis() - start;
  }

  // ===== RECORD LINKAGE STRATEGIES
  // the following two methods implement different record matching
  // strategies. the first is used for deduplication, where we simply
  // want all matches above the thresholds. the second is used for
  // record linkage, to implement a simple greedy matching algorithm
  // where we choose the best alternative above the threshold for each
  // record.

  // other, more advanced possibilities exist for record linkage, but
  // they are not implemented yet. see the links below for more
  // information.
  
  // http://code.google.com/p/duke/issues/detail?id=55
  // http://research.microsoft.com/pubs/153478/msr-report-1to1.pdf
  
  /**
   * Passes on all matches found.
   */
  protected void compareCandidatesSimple(Record record,
                                         Collection<Record> candidates) {
    boolean found = false;
    for (Record candidate : candidates) {
      if (isSameAs(record, candidate))
        continue;
    	  
      double prob = compare(record, candidate);
      if (prob > config.getThreshold()) {
        found = true;
        registerMatch(record, candidate, prob);
      } else if (config.getMaybeThreshold() != 0.0 &&
                 prob > config.getMaybeThreshold()) {
        found = true; // I guess?
        registerMatchPerhaps(record, candidate, prob);
      }
    }
    if (!found)
      registerNoMatchFor(record);
  }

  /**
   * Passes on only the best match for each record.
   */
  protected void compareCandidatesBest(Record record,
                                         Collection<Record> candidates) {
    double max = 0.0;
    Record best = null;

    // go through all candidates, and find the best
    for (Record candidate : candidates) {
      if (isSameAs(record, candidate))
        continue;
      
      double prob = compare(record, candidate);
      if (prob > max) {
        max = prob;
        best = candidate;
      }
    }

    // pass on the best match, if any 
    if (max > config.getThreshold())
      registerMatch(record, best, max);
    else if (config.getMaybeThreshold() != 0.0 &&
             max > config.getMaybeThreshold())
      registerMatchPerhaps(record, best, max);
    else
      registerNoMatchFor(record);
  }
  
  /**
   * Compares two records and returns the probability that they
   * represent the same real-world entity.
   */
  public double compare(Record r1, Record r2) {
    comparisons++;
    double prob = 0.5;
    for (String propname : r1.getProperties()) {
      Property prop = config.getPropertyByName(propname);
      if (prop == null)
        continue; // means the property is unknown
      if (prop.isIdProperty() || prop.isIgnoreProperty())
        continue;

      Collection<String> vs1 = r1.getValues(propname);
      Collection<String> vs2 = r2.getValues(propname);
      if (vs1 == null || vs1.isEmpty() || vs2 == null || vs2.isEmpty())
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
            throw new DukeException("Comparison of values '" + v1 + "' and "+
                                    "'" + v2 + "' with " +
                                    prop.getComparator() + " failed", e);
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
  public void close() {
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

  private void startProcessing() {
    long start = System.currentTimeMillis();
    for (MatchListener listener : listeners)
      listener.startProcessing();
    callbacks += (System.currentTimeMillis() - start);
  }

  private void endProcessing() {
    long start = System.currentTimeMillis();
    for (MatchListener listener : listeners)
      listener.endProcessing();
    callbacks += (System.currentTimeMillis() - start);
  }

  private void batchReady(int size) {
    long start = System.currentTimeMillis();
    for (MatchListener listener : listeners)
      listener.batchReady(size);
    callbacks += (System.currentTimeMillis() - start);
  }

  private void batchDone() {
    long start = System.currentTimeMillis();
    for (MatchListener listener : listeners)
      listener.batchDone();
    callbacks += (System.currentTimeMillis() - start);
  }
  
  /**
   * Records the statement that the two records match.
   */
  private void registerMatch(Record r1, Record r2, double confidence) {
    long start = System.currentTimeMillis();
    for (MatchListener listener : listeners)
      listener.matches(r1, r2, confidence);
    callbacks += (System.currentTimeMillis() - start);
  }

  /**
   * Records the statement that the two records may match.
   */
  private void registerMatchPerhaps(Record r1, Record r2, double confidence) {
    long start = System.currentTimeMillis();
    for (MatchListener listener : listeners)
      listener.matchesPerhaps(r1, r2, confidence);
    callbacks += (System.currentTimeMillis() - start);
  }

  /**
   * Notifies listeners that we found no matches for this record.
   */
  private void registerNoMatchFor(Record current) {
    long start = System.currentTimeMillis();
    for (MatchListener listener : listeners)
      listener.noMatchFor(current);
    callbacks += (System.currentTimeMillis() - start);
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

  // ===== THREADS

  /**
   * The thread that actually runs parallell matching. It holds the
   * thread's share of the current batch.
   */
  class MatchThread extends Thread {
    private Collection<Record> records;
    private boolean matchall;

    public MatchThread(int threadno, int recordcount, boolean matchall) {
      super("MatchThread " + threadno);
      this.records = new ArrayList(recordcount);
      this.matchall = matchall;
    }

    public void run() {
      for (Record record : records)
        match(record, matchall);
    }

    public void addRecord(Record record) {
      records.add(record);
    }
  }

  // ===== PERFORMANCE PROFILING

  public class Profiler extends AbstractMatchListener {
    private long processing_start;
    private long batch_start;
    private int batch_size;
    private int records;
    private PrintWriter out;

    public Profiler() {
      this.out = new PrintWriter(System.out);
    }

    /**
     * Sets Writer to receive performance statistics. Defaults to
     * System.out.
     */
    public void setOutput(Writer outw) {
      this.out = new PrintWriter(outw);
    }

    public void startProcessing() {
      processing_start = System.currentTimeMillis();
      System.out.println("Duke version " + Duke.getVersionString());
      System.out.println(getDatabase());
      System.out.println("Threads: " + getThreads());
    }
    
    public void batchReady(int size) {
      batch_start = System.currentTimeMillis();
      batch_size = size;
    }
  
    public void batchDone() {
      records += batch_size;
      int rs = (int) ((1000.0 * batch_size) /
                      (System.currentTimeMillis() - batch_start));
      System.out.println("" + records + " processed, " + rs +
                         " records/second; comparisons: " +
                         getComparisonCount());
    }
    
    public void endProcessing() {
      long end = System.currentTimeMillis();
      double rs = (1000.0 * records) / (end - processing_start);
      System.out.println("Run completed, " + (int) rs + " records/second");
      System.out.println("" + records + " records total in " +
                         ((end - processing_start) / 1000) + " seconds");

      long total = srcread + indexing + searching + comparing + callbacks;
      System.out.println("Reading from source: " +
                         seconds(srcread) + " (" +
                         percent(srcread, total) + "%)");
      System.out.println("Indexing: " +
                         seconds(indexing) + " (" +
                         percent(indexing, total) + "%)");
      System.out.println("Searching: " +
                         seconds(searching) + " (" +
                         percent(searching, total) + "%)");
      System.out.println("Comparing: " +
                         seconds(comparing) + " (" +
                         percent(comparing, total) + "%)");
      System.out.println("Callbacks: " +
                         seconds(callbacks) + " (" +
                         percent(callbacks, total) + "%)");
      System.out.println();
      Runtime r = Runtime.getRuntime();
      System.out.println("Total memory: " + r.totalMemory() + ", " +
                         "free memory: " + r.freeMemory() + ", " +
                         "used memory: " + (r.totalMemory() - r.freeMemory()));
    }

    private String seconds(long ms) {
      return "" + (int) (ms / 1000);
    }
    
    private String percent(long ms, long total) {
      return "" + (int) ((double) (ms * 100) / (double) total);
    }
  }  
}