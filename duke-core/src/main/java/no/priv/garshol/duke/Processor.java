
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
import no.priv.garshol.duke.utils.DefaultRecordIterator;

/**
 * The class that implements the actual deduplication and record
 * linkage logic.
 */
public class Processor {
  private Configuration config;
  private Collection<MatchListener> listeners;
  private Logger logger;
  private List<Property> proporder;
  private double[] accprob;
  private int threads;
  private Database database1;
  private Database database2;
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
    this(config, config.getDatabase(1, overwrite));
    database2 = config.getDatabase(2, overwrite);
  }

  /**
   * Creates a new processor, bound to the given database.
   */
  public Processor(Configuration config, Database database) {
    this.config = config;
    this.database1 = database;
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
   * Returns the Database in which the Duke index is stored. This will
   * be the Lucene index if you are using the Lucene database.
   */
  public Database getDatabase() {
    return database1;
  }

  /**
   * Returns the Database in which the Duke index is stored for the
   * given group in record linkage mode. This will be the Lucene index
   * if you are using the Lucene database.
   * @param group Must be 1 or 2.
   */
  public Database getDatabase(int group) {
    if (group == 1)
      return database1;
    else if (group == 2)
      return database2;
    throw new DukeException("Unknown group " + group);
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
    int count = 0;
    startProcessing();

    Iterator<DataSource> it = sources.iterator();
    while (it.hasNext()) {
      DataSource source = it.next();
      source.setLogger(logger);

      RecordIterator it2 = source.getRecords();
      try {
        Collection<Record> batch = new ArrayList();
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

        if (!batch.isEmpty()) {
          deduplicate(batch);
          it2.batchProcessed();
        }
      } finally {
        it2.close();
      }
    }

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
      database1.index(record);

    database1.commit();
    indexing += System.currentTimeMillis() - start;

    // then match
    match(records, true);

    batchDone();
  }

  private void match(Collection<Record> records, boolean matchall) {
    if (threads == 1)
      for (Record record : records)
        match(1, record, matchall);
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

    // start with source 1
    for (Collection<Record> batch : makeBatches(sources1, batch_size)) {
      index(1, batch);
      if (hasTwoDatabases())
        linkBatch(2, batch, matchall);
    }

    // then source 2
    for (Collection<Record> batch : makeBatches(sources2, batch_size)) {
      if (hasTwoDatabases())
        index(2, batch);
      linkBatch(1, batch, matchall);
    }

    endProcessing();
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
    linkRecords(1, sources, matchall, batch_size);
  }

  /**
   * Retrieve new records from data sources, and match them to
   * previously indexed records in the given database. This method
   * does <em>not</em> index the new records.
   * @param dbno Which database to match against.
   * @param matchall If true, all matching records are accepted. If false,
   *                 only the single best match for each record is accepted.
   * @param batch_size The batch size to use.
   * @since 1.3
   */
  public void linkRecords(int dbno, Collection<DataSource> sources,
                          boolean matchall, int batch_size) {
    for (DataSource source : sources) {
      source.setLogger(logger);

      Collection<Record> batch = new ArrayList(batch_size);
      RecordIterator it = source.getRecords();
      while (it.hasNext()) {
        batch.add(it.next());
        if (batch.size() == batch_size) {
          linkBatch(dbno, batch, matchall);
          batch.clear();
        }
      }
      it.close();

      if (!batch.isEmpty())
        linkBatch(dbno, batch, matchall);
    }

    endProcessing();
  }

  private void linkBatch(int dbno, Collection<Record> batch, boolean matchall) {
    batchReady(batch.size());
    for (Record r : batch)
      match(dbno, r, matchall);
    batchDone();
  }

  /**
   * Index all new records from the given data sources. This method
   * does <em>not</em> do any matching.
   * @since 0.4
   */
  public void index(Collection<DataSource> sources, int batch_size) {
    index(1, sources, batch_size);
  }

  /**
   * Index all new records from the given data sources into the given
   * database. This method does <em>not</em> do any matching.
   * @since 1.3
   */
  public void index(int dbno, Collection<DataSource> sources, int batch_size) {
    Database thedb = getDB(dbno);

    int count = 0;
    for (DataSource source : sources) {
      source.setLogger(logger);

      RecordIterator it2 = source.getRecords();
      while (it2.hasNext()) {
        Record record = it2.next();
        if (logger.isDebugEnabled())
          logger.debug("Indexing record " + record);
        thedb.index(record);
        count++;
        if (count % batch_size == 0)
          batchReady(batch_size);
      }
      it2.close();
    }
    if (count % batch_size == 0)
      batchReady(count % batch_size);
    thedb.commit();
  }

  /**
   * Index the records into the given database. This method does
   * <em>not</em> do any matching.
   * @since 1.3
   */
  public void index(int dbno, Collection<Record> batch) {
    Database thedb = getDB(dbno);

    for (Record r : batch) {
      if (logger.isDebugEnabled())
        logger.debug("Indexing record " + r);
      thedb.index(r);
    }
    thedb.commit();
  }

  /**
   * Returns the number of records that have been compared.
   */
  public long getComparisonCount() {
    return comparisons;
  }

  private void match(int dbno, Record record, boolean matchall) {
    long start = System.currentTimeMillis();
    Collection<Record> candidates = getDB(dbno).findCandidateMatches(record);
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
    if (logger.isDebugEnabled()) {
      logger.debug("Best candidate at " + max + " is " + best);
    }
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
    database1.close();
    if (hasTwoDatabases())
      database2.close();
  }

  // ===== INTERNALS

  private Iterable<Collection<Record>> makeBatches(Collection<DataSource> sources, int batch_size) {
    return new BatchIterator(sources, batch_size);
  }

  static class BatchIterator implements Iterable<Collection<Record>>,
                                        Iterator<Collection<Record>> {
    private BasicIterator it;
    private int batch_size;

    public BatchIterator(Collection<DataSource> sources, int batch_size) {
      this.it = new BasicIterator(sources);
      this.batch_size = batch_size;
    }

    public boolean hasNext() {
      return it.hasNext();
    }

    public Collection<Record> next() {
      Collection<Record> batch = new ArrayList();
      while (it.hasNext())
        batch.add(it.next());
      return batch;
    }

    public Iterator<Collection<Record>> iterator() {
      return this;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  static class BasicIterator implements Iterator<Record> {
    private Iterator<DataSource> srcit;
    private RecordIterator recit;

    public BasicIterator(Collection<DataSource> sources) {
      this.srcit = sources.iterator();
      findNextIterator();
    }

    public boolean hasNext() {
      return recit.hasNext();
    }

    public Record next() {
      Record r = recit.next();
      if (!recit.hasNext())
        findNextIterator();
      return r;
    }

    private void findNextIterator() {
      if (srcit.hasNext()) {
        DataSource src = srcit.next();
        recit = src.getRecords();
      } else
        recit = new DefaultRecordIterator(Collections.EMPTY_SET.iterator());
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public boolean hasTwoDatabases() {
    return database2 != null;
  }

  private Database getDB(int no) {
    if (no == 1)
      return database1;
    else if (no == 2)
      return database2;
    else
      throw new DukeException("Unknown database " + no);
  }

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
    if (logger.isDebugEnabled())
      logger.debug("Start processing with " + database1 + " and " + database2);

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
        match(1, record, matchall);
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
      if (hasTwoDatabases())
        System.out.println(database2);
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
