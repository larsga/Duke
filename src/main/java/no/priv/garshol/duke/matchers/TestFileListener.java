
package no.priv.garshol.duke.matchers;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Collection;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.Configuration;

public class TestFileListener extends AbstractMatchListener {
  private Collection<Property> idprops;
  private List<Property> props;
  private Map<String, Link> links;
  private int notintest;
  private int missed; // RL mode only
  private boolean debug;
  private boolean quiet; // true means no output whatever (default: false)
  private boolean linkage;
  private boolean showmatches; // means other listener is showing matches
  private Processor processor;
  private Database database;
  private double f;

  /**
   * Creates a test file listener.
   * @param linkage True iff in record linkage mode.
   */
  public TestFileListener(String testfile, Configuration config,
                          boolean debug, Processor processor, boolean linkage,
                          boolean showmatches)
    throws IOException {
    this.idprops = config.getIdentityProperties();
    this.props = config.getProperties();
    this.links = load(testfile);
    this.debug = debug;
    this.processor = processor;
    this.database = processor.getDatabase();
    this.linkage = linkage;
    this.showmatches = showmatches;
  }

  public void setQuiet(boolean quiet) {
    this.quiet = quiet;
  }

  public double getFNumber() {
    return f;
  }

  public void close() throws IOException {
    int correct = 0;
    int correctfound = 0;
    int wrong = 0;
    int wrongfound = 0;
      
    for (Link link : links.values()) {
      if (link.correct) {
        correct++;
        if (link.asserted)
          correctfound++;
        else if (debug) {
          // we missed this one
          Record r1 = database.findRecordById(link.id1);
          Record r2 = database.findRecordById(link.id2);
          if (r1 != null && r2 != null) {
            if (!quiet && !showmatches)
              PrintMatchListener.show(r1, r2, processor.compare(r1, r2),
                                      "\nNOT FOUND", props);
          } else if (!quiet && !showmatches) {
            System.out.println("\nIDENTITIES IN TEST FILE NOT FOUND IN DATA");
            System.out.println("ID1: " + link.id1 + " -> " + r1);
            System.out.println("ID2: " + link.id2 + " -> " + r2);
          }
        }
      } else {
        wrong++;
        if (link.asserted)
          wrongfound++;
      }
    }

    int total = correctfound + wrongfound + notintest;
    if (!quiet) {
      System.out.println("");
      System.out.println("Correct links found: " + correctfound + " / " +
                         correct + " (" + percent(correctfound, correct) + "%)");
      System.out.println("Wrong links found: " + wrongfound + " / " +
                         wrong + " (" + percent(wrongfound, wrong) + "%)");
      System.out.println("Unknown links found: " + notintest);
      System.out.println("Percent of links correct " +
                         percent(correctfound, total) +
                         "%, wrong " +
                         percent(wrongfound, total) +
                         "%, unknown " +
                         percent(notintest, total) + "%");
      if (missed > 0)
        System.out.println("Records with no link: " + missed);
    }

    double precision = ((double) correctfound) / total;
    double recall = ((double) correctfound) / correct;
    if (correctfound == 0)
      f = 0.0;
    else
      f = 2 * ((precision * recall) / (precision + recall));
    if (!quiet)
      System.out.println("Precision " + percent(correctfound, total) +
                         "%, recall " + percent(correctfound, correct) +
                         "%, f-number " + f);
  }
    
  public synchronized void matches(Record r1, Record r2, double confidence) {
    boolean found = false;
    for (Property p : idprops)
      for (String id1 : r1.getValues(p.getName()))
        for (String id2 : r2.getValues(p.getName())) {
          if (id1.compareTo(id2) < 0) {
            String tmp = id1;
            id1 = id2;
            id2 = tmp;
          }

          Link link = links.get(id1 + "," + id2);
          if (link != null) {
            found = true;
            link.asserted();
            if (!link.correct && debug && !showmatches)
              PrintMatchListener.show(r1, r2, confidence, "\nINCORRECT",
                                      props);
            break;
          }
        }

    if (!found) {
      notintest++;
      if (debug && !showmatches)
        PrintMatchListener.show(r1, r2, confidence, "\nNOT IN TEST FILE",
                                props);
    }
  }
   
  // when we don't find any matches for a record.
  public synchronized void noMatchFor(Record record) {
    if (!quiet && linkage && !showmatches) {
      System.out.println("\nNO MATCHING RECORD");
      System.out.println(PrintMatchListener.toString(record));
    }

    // GRRR! we can't work out here whether this miss is in the test file
    // or not
    missed++;
  }

  private String percent(int part, int total) {
    int p = (int) (((double) part * 1000) / (double) total);
    return "" + (p / 10.0);
  }

  private Map<String, Link> load(String testfile) throws IOException {
    Map<String, Link> links = new HashMap();
    BufferedReader reader = new BufferedReader(new FileReader(testfile));
    String line = reader.readLine();
    while (line != null) {
      int pos = line.indexOf(',');

      String id1 = line.substring(1, pos);
      String id2 = line.substring(pos + 1, line.length());
      if (id1.compareTo(id2) < 0) {
        String tmp = id1;
        id1 = id2;
        id2 = tmp;
      }

      links.put(id1 + "," + id2, new Link(id1, id2, line.charAt(0) == '+'));
        
      line = reader.readLine();
    }

    reader.close();
    return links;
  }

  // FIXME: could we get rid of this and use the duke/Link class instead?
  static class Link {
    private String id1; // this is always lexicographically lower than id2
    private String id2;
    private boolean correct;  // is this link correct?
    private boolean asserted; // did Duke assert this link?

    public Link(String id1, String id2, boolean correct) {
      this.id1 = id1;
      this.id2 = id2;
      this.correct = correct;
    }

    public void asserted() {
      asserted = true;
    }
  }
}

