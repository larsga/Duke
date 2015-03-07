
package no.priv.garshol.duke.databases;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class PriorityQueueTest {

  @Test
  public void test100() {
    KeyValueDatabase.Score scores[] = new KeyValueDatabase.Score[100];
    for (int ix = 0; ix < scores.length; ix++) {
      scores[ix] = new KeyValueDatabase.Score(ix);
      scores[ix].score = (double) ix;
    }
    KeyValueDatabase.PriorityQueue pq =
      new KeyValueDatabase.PriorityQueue(scores);

    for (int ix = 0; ix < scores.length; ix++) {
      KeyValueDatabase.Score score = pq.next();
      assertEquals((99 - ix), (int) score.score);
    }
  }
}