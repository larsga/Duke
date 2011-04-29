
package no.priv.garshol.duke;

import java.util.Collection;

public class PrintMatchListener implements MatchListener {
  private int count;
  private boolean showmatches;
  private boolean progress;
  
  public PrintMatchListener(boolean showmatches, boolean progress) {
    this.count = 0;
    this.showmatches = showmatches;
    this.progress = progress;
  }
  
  public int getMatchCount() {
    return count;
  }
  
  public void matches(Record r1, Record r2, double confidence) {
    count++;
    if (showmatches)
      show(r1, r2, confidence);
    if (count % 1000 == 0 && progress)
      System.out.println("" + count + "  matches");
  }

  public static void show(Record r1, Record r2, double confidence) {
    System.out.println("MATCH " + confidence);      
    System.out.println(toString(r1));
    System.out.println(toString(r2));
  }

  public static String toString(Record r) {
    StringBuffer buf = new StringBuffer();
    for (String p : r.getProperties()) {
      Collection<String> vs = r.getValues(p);
      if (vs == null)
        continue;
      
      buf.append(p + ": ");          
      for (String v : vs)
        buf.append("'" + v + "', ");
    }
    return buf.toString();
  }
}
