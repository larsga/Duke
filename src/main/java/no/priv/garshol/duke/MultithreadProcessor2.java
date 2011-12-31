
package no.priv.garshol.duke;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;

import no.priv.garshol.duke.matchers.MatchListener;

/**
 * The first attempt didn't actually increase throughput, possibly
 * because of serialization of Lucene searches. We therefore try
 * multithreading the property comparison stage instead.
 */
public class MultithreadProcessor2 extends Processor {
  private Queue<Task> queue;
  private int threads;
  private int finished; // number of threads finished
  private boolean stopped;
  private static int DEFAULT_THREAD_COUNT = 1;
  private MatchListener filter;
  private int count;
  
  public MultithreadProcessor2(Configuration config) throws IOException {
    this(config, false);
  }

  public MultithreadProcessor2(Configuration config, boolean overwrite)
    throws IOException {
    super(config, overwrite);
    this.threads = DEFAULT_THREAD_COUNT;
    this.queue = new ArrayDeque();
    this.stopped = false;
  }

  /**
   * Tells the processor what number of threads to use.
   */
  public void setThreadCount(int threads) {
    this.threads = threads;
  } 
  
  protected void compareCandidates(Record record, Collection<Record> candidates,
                                   MatchListener filter) {
    this.filter = filter; // we blithely assume this does not change
    synchronized (this) {
      queue.add(new Task(record, candidates));
      count++;
      if (count % 100 == 0)
        System.out.println("" + count + " records arrived, " + queue.size() +
                           " in queue");
    }
  }

  public void deduplicate(Collection<DataSource> sources, int batch_size)
    throws IOException {
    startThreads();
    super.deduplicate(sources, batch_size);
  }  

  // called by the thread to do the actual work
  protected void compareCandidates(Task task) {
    super.compareCandidates(task.record, task.candidates, filter);
  }
  
  public void close() throws IOException {
    System.out.println("Closing processor");
    stopped = true;

    // wait for all threads to finish
    while (finished < threads) {
      try {
        System.out.println("Waiting, finished " + finished + ", queue" +
                           queue.size());
        Thread.sleep(10); 
      } catch (InterruptedException e) {
      }
    }

    // now we can close and finish up
    database.close();
  }

  private void startThreads() {
    for (int ix = 0; ix < threads; ix++)
      new MatchingThread(this, ix).start();
  }

  class MatchingThread extends Thread {
    private MultithreadProcessor2 processor;

    public MatchingThread(MultithreadProcessor2 processor, int ix) {
      super("MatchingThread " + ix);
      this.processor = processor;
    }
    
    public void run() {
      int tasks = 0;
      while (!stopped || !queue.isEmpty()) {
        // get a batch of records, and process it

        Task task;
        synchronized (processor) {
          // get record + candidates
          task = queue.poll();
        }
        while (task != null) {
          tasks++;
          processor.compareCandidates(task);
          synchronized (processor) {
            // get record + candidates
            task = queue.poll();
          }
        }
        
        // wait for more records to arrive
        try {
          sleep(10); 
        } catch (InterruptedException e) {
        }
      }

      // okay, this thread has no more work left to do
      synchronized (processor) {
        finished++;
        System.out.println("Thread finished: " + finished + ", " + tasks +
                           "tasks");
      }
    }
  }

  static class Task {
    public Record record;
    public Collection<Record> candidates;

    public Task(Record record, Collection<Record> candidates) {
      this.record = record;
      this.candidates = candidates;
    }
  }
}