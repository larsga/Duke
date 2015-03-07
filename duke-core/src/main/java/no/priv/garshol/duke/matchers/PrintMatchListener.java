
package no.priv.garshol.duke.matchers;

import java.util.List;
import java.util.Collection;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;

/**
 * Match listener which prints events to standard out. Used by the
 * command-line client. Also contains utility methods for printing
 * records nicely.
 */
public class PrintMatchListener extends AbstractMatchListener {
  private int matches;
  private int records;
  private int nonmatches; // only counted in record linkage mode
  private boolean showmaybe;
  private boolean showmatches;
  private boolean progress;
  private boolean linkage; // means there's a separate indexing step
  private boolean pretty;
  private List<Property> properties;

  /**
   * Creates a new listener.
   * @param showmatches Whether to display matches. (On cmd-line: --showmatches)
   * @param showmaybe Whether to display maybe-matches. --showmaybe
   * @param progress Whether to display progress reports. --progress
   * @param linkage True iff in record linkage mode.
   * @param pretty Whether to pretty-print records (not compact).
   */
  public PrintMatchListener(boolean showmatches, boolean showmaybe,
                            boolean progress, boolean linkage,
                            List<Property> properties, boolean pretty) {
    this.matches = 0;
    this.records = 0;
    this.showmatches = showmatches;
    this.showmaybe = showmaybe;
    this.progress = progress;
    this.linkage = linkage;
    this.properties = properties;
    this.pretty = pretty;
  }
  
  public int getMatchCount() {
    return matches;
  }

  public void batchReady(int size) {
    if (progress)
      System.out.println("Records: " + records);
    records += size;
  }
  
  public void matches(Record r1, Record r2, double confidence) {
    matches++;
    if (showmatches) {
      if (pretty)
        prettyCompare(r1, r2, confidence, "\nMATCH", properties);
      else
        show(r1, r2, confidence, "\nMATCH", properties);
    }
    if (matches % 1000 == 0 && progress)
      System.out.println("" + matches + "  matches");
  }

  public void matchesPerhaps(Record r1, Record r2, double confidence) {
    if (showmaybe) {
      if (pretty)
        prettyCompare(r1, r2, confidence, "\nMAYBE MATCH", properties);
      else
        show(r1, r2, confidence, "\nMAYBE MATCH", properties);
    }
  }

  public void endProcessing() {
    if (progress) {
      System.out.println("");
      System.out.println("Total records: " + records);
      System.out.println("Total matches: " + matches);
      System.out.println("Total non-matches: " + nonmatches);
    }
  }

  public void noMatchFor(Record record) {
    nonmatches++;
    if (showmatches && linkage)
      System.out.println("\nNO MATCH FOR:\n" + toString(record, properties));
  }
  
  // =====
  
  public static void show(Record r1, Record r2, double confidence,
                          String heading, List<Property> props) {
    System.out.println(heading + " " + confidence);      
    System.out.println(toString(r1, props));
    System.out.println(toString(r2, props));
  }

  public static void show(Record r1, Record r2, double confidence,
                          String heading, List<Property> props,
                          boolean pretty) {
    if (pretty)
      prettyCompare(r1, r2, confidence, heading, props);
    else
      show(r1, r2, confidence, heading, props);
  }
  
  // mostly used in error messages
  public static String toString(Record r) {
    StringBuffer buf = new StringBuffer();
    for (String p : r.getProperties()) {
      Collection<String> vs = r.getValues(p);
      if (vs == null || vs.isEmpty())
        continue;
      
      buf.append(p + ": ");          
      for (String v : vs)
        buf.append("'" + v + "', ");
    }

    //buf.append(";;; " + r);
    return buf.toString();
  }
  
  public static String toString(Record r, List<Property> props) {
    StringBuffer buf = new StringBuffer();
    for (Property p : props) {
      Collection<String> vs = r.getValues(p.getName());
      if (vs == null || vs.isEmpty())
        continue;
      
      buf.append(p.getName() + ": ");          
      for (String v : vs)
        buf.append("'" + v + "', ");
    }

    //buf.append(";;; " + r);
    return buf.toString();
  }
  
  public static void prettyCompare(Record r1, Record r2, double confidence,
                                   String heading, List<Property> props) {
    System.out.println(heading + " " + confidence);

    for (Property p : props) {
      String prop = p.getName();
      if ((r1.getValues(prop) == null || r1.getValues(prop).isEmpty()) &&
          (r2.getValues(prop) == null || r2.getValues(prop).isEmpty()))
        continue;
      
      System.out.println(prop);
      System.out.println("  " + value(r1, prop));
      System.out.println("  " + value(r2, prop));
    }
  }

  public static void prettyPrint(Record r, List<Property> props) {
    for (Property p : props) {
      String prop = p.getName();
      if (r.getValues(prop) == null || r.getValues(prop).isEmpty())
        continue;
      
      System.out.println(prop + ": " + value(r, prop));
    }
  }
  
  public static void htmlCompare(Record r1, Record r2, double confidence,
                                   String heading, List<Property> props) {
    System.out.println("<p>" + heading + " " + confidence + "</p>");

    System.out.println("<table>");
    for (Property p : props) {
      String prop = p.getName();
      if ((r1.getValues(prop) == null || r1.getValues(prop).isEmpty()) &&
          (r2.getValues(prop) == null || r2.getValues(prop).isEmpty()))
        continue;

      System.out.println("<tr><td>" + prop);
      System.out.println("<td>" + value(r1, prop));
      System.out.println("<td>" + value(r2, prop));
    }
    System.out.println("</table>");
  }
  
  private static String value(Record r, String p) {
    Collection<String> vs = r.getValues(p);
    if (vs == null)
      return "<null>";
    if (vs.isEmpty())
      return "<null>";

    StringBuffer buf = new StringBuffer();
    for (String v : vs) {
      buf.append("'");
      buf.append(v);
      buf.append("', ");
    }
      
    return buf.toString();
  }
}
