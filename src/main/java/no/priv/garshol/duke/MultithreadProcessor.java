
package no.priv.garshol.duke;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;

/**
 * Experimental implementation of Processor which uses multiple
 * threads to speed up processing.
 *
 * <p>The basic principle is that (for now) the normal loop pulling
 * records from the data sources and indexing them runs in the main
 * loop. 
 */
public class MultithreadProcessor extends Processor {
  private Queue<Record> queue;
  private int threads;
  private int finished; // number of threads finished
  private boolean stopped;
  private static int DEFAULT_THREAD_COUNT = 1;
  
  public MultithreadProcessor(Configuration config) throws IOException {
    this(config, false);
  }

  public MultithreadProcessor(Configuration config, boolean overwrite)
    throws IOException {
    super(config, overwrite);
    this.threads = DEFAULT_THREAD_COUNT;
    this.queue = new ArrayDeque();
    this.stopped = false;
    startThreads();
  }

  /**
   * Tells the processor what number of threads to use.
   */
  public void setThreadCount(int threads) {
    this.threads = threads;
  }

  public void deduplicate(Collection<Record> records) {
    synchronized (this) {
      queue.addAll(records);
    }
    System.out.println("Batch arrived: " + records.size() + " " + queue.size());
  }

  // called by the thread to do the actual work
  private void deduplicate2(Collection<Record> records) {
    super.deduplicate(records);
  }
  
  public void close() throws IOException {
    stopped = true;

    // wait for all threads to finish
    while (finished < threads) {
      try {
        Thread.sleep(10); 
      } catch (InterruptedException e) {
      }
    }

    // now we can close and finish up
    database.close();
  }

  private void startThreads() {
    for (int ix = 0; ix < threads; ix++)
      new MatchingThread(this).start();
  }

  class MatchingThread extends Thread {
    private MultithreadProcessor processor;

    public MatchingThread(MultithreadProcessor processor) {
      this.processor = processor;
    }
    
    public void run() {
      while (!stopped || !queue.isEmpty()) {
        // get a batch of records, and process it
        Collection<Record> batch = new ArrayList(100);

        // collect batch
        synchronized (processor) {
          Record r = queue.poll();
          while (r != null && batch.size() < 100) {
            batch.add(r);
            r = queue.poll();
          }

        }

        // process batch
        if (!batch.isEmpty())
          processor.deduplicate2(batch);
        
        // wait for more records to arrive
        try {
          sleep(10); 
        } catch (InterruptedException e) {
        }
      }

      // okay, this thread has no more work left to do
      synchronized (processor) {
        finished++;
      }
    }
  }
}