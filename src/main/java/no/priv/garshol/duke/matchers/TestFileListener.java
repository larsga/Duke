
package no.priv.garshol.duke.matchers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.priv.garshol.duke.Link;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.LinkKind;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.LinkStatus;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.InMemoryLinkDatabase;
import no.priv.garshol.duke.utils.LinkDatabaseUtils;

/**
 * A match listener for evaluating Duke configurations against a set
 * of known correct mappings.  Used by the command-line client.
 */
public class TestFileListener extends AbstractMatchListener {
  private Collection<Property> idprops;
  private List<Property> props;
  private LinkDatabase golddb; // gold standard to test against
  private LinkDatabase dukedb; // the links Duke has found
  private boolean debug;
  private boolean quiet; // true means no output whatever (default: false)
  private boolean linkage;
  private boolean showmatches; // means other listener is showing matches
  private boolean pretty;      // whether to pretty-print records
  private boolean pessimist;   // true: assume unknown links are false,
                               // false: ignore unknown links in F calcs
  private Processor processor;
  private Database database;

  // statistics
  private int missed;
  private int wrongfound;
  private int unknown;
  private double f;

  /**
   * Creates a test file listener.
   */
  public TestFileListener(String testfile, Configuration config,
                          boolean debug, Processor processor,
                          boolean showmatches, boolean pretty)
    throws IOException {
    InMemoryLinkDatabase testdb = new InMemoryLinkDatabase();
    //testdb.setDoInference(true);
    LinkDatabaseUtils.loadTestFile(testfile, testdb);
    init(testdb, config, debug, processor, showmatches, pretty);
  }

  /**
   * Creates a test file listener.
   * @since 1.1
   */
  public TestFileListener(LinkDatabase linkdb, Configuration config,
                          boolean debug, Processor processor,
                          boolean showmatches, boolean pretty) {
    init(linkdb, config, debug, processor, showmatches, pretty);
  }

  private void init(LinkDatabase linkdb, Configuration config,
                    boolean debug, Processor processor,
                    boolean showmatches, boolean pretty) {
    this.golddb = linkdb;
    this.dukedb = new InMemoryLinkDatabase();
    //((InMemoryLinkDatabase) this.dukedb).setDoInference(true);
    this.idprops = config.getIdentityProperties();
    this.props = config.getProperties();
    this.debug = debug;
    this.processor = processor;
    this.database = processor.getDatabase();
    this.linkage = !config.isDeduplicationMode();
    this.showmatches = showmatches;
    this.pretty = pretty;
  }

  public void setQuiet(boolean quiet) {
    this.quiet = quiet;
  }

  public void setPessimistic(boolean pessimist) {
    this.pessimist = pessimist;
  }

  public double getFNumber() {
    return f;
  }

  public boolean isEmpty() {
    return golddb.getAllLinks().isEmpty();
  }

  public synchronized void matches(Record r1, Record r2, double confidence) {
    String id1 = getid(r1);
    String id2 = getid(r2);

    Link link = golddb.inferLink(id1, id2);
    if (link == null) {
      unknown++; // we don't know if this one is right or not
      if (debug && !showmatches)
        PrintMatchListener.show(r1, r2, confidence, "\nNOT IN TEST FILE",
                                props, pretty);
    } else if (link.getKind() == LinkKind.SAME)
      // no counting now; we do that when we're done
      dukedb.assertLink(new Link(id1, id2, LinkStatus.INFERRED, LinkKind.SAME,
                                 confidence));
    else if (link.getKind() == LinkKind.DIFFERENT) {
      wrongfound++; // we found it, but it's not right

      if (debug && !showmatches)
        PrintMatchListener.show(r1, r2, confidence, "\nINCORRECT",
                                props, pretty);
    } else {
      unknown++; // we don't know if this one is right or not
      if (debug && !showmatches)
        PrintMatchListener.show(r1, r2, confidence, "\nUNKNOWN LINK TYPE",
                                props, pretty);
    }
  }

  public synchronized void noMatchFor(Record r) {
    // we missed all of the correct links for this record (if any).
    // count, and tell the user.
    for (Link link : golddb.getAllLinksFor(getid(r))) {
      if (link.getKind() != LinkKind.SAME)
        continue; // it's a bad link, so never mind

      missed++;

      Record r1 = database.findRecordById(link.getID1());
      Record r2 = database.findRecordById(link.getID2());
      if (r1 != null && r2 != null) {
        if (debug && !showmatches)
          PrintMatchListener.show(r1, r2, processor.compare(r1, r2),
                                  "\nNOT FOUND", props, pretty);
      } else if (debug && !showmatches) {
        System.out.println("\nIDENTITIES IN TEST FILE NOT FOUND IN DATA");
        System.out.println("ID1: " + link.getID1() + " -> " + r1);
        System.out.println("ID2: " + link.getID2() + " -> " + r2);
      }
    }
  }

  public void endProcessing() {
    // count the links we've found, with inferences included
    int correctfound = dukedb.getAllLinks().size(); // only correct ones here

    // count the links with known answers
    int correct = 0;
    int wrong = 0;
    for (Link link : golddb.getAllLinks()) {
      if (link.getKind() == LinkKind.SAME)
        correct++;
      else
        wrong++;
    }
    wrong *= 2; // need to also count id2, id1 links, not just id1, id2

    // compute total
    int total = correctfound + wrongfound;
    if (pessimist)
      total += unknown;

    // compute F
    double precision = ((double) correctfound) / total;
    double recall = ((double) correctfound) / correct;
    if (correctfound == 0)
      f = 0.0;
    else
      f = 2 * (precision * recall) / (precision + recall);

    // tell the user
    if (!quiet) {
      System.out.println("");
      System.out.println("Correct links found: " + correctfound + " / " +
                         correct + " (" + percent(correctfound, correct) + "%)");
      System.out.println("Wrong links found: " + wrongfound + " / " +
                         wrong + " (" + percent(wrongfound, wrong) + "%)");
      System.out.println("Unknown links found: " + unknown);
      System.out.println("Percent of links correct " +
                         percent(correctfound, total) +
                         "%, wrong " +
                         percent(wrongfound, total) +
                         "%, unknown " +
                         percent(unknown, total) + "%");
      if (missed > 0)
        System.out.println("Records with no link: " + missed);

      System.out.println("Precision " + (precision * 100) +
                         "%, recall " + (recall * 100) +
                         "%, f-number " + f);
    }

    if (f > 1.0) {
      System.out.println("===== GOLDDB ======================================");
      System.out.println(golddb);
      for (Link link : golddb.getAllLinks())
        System.out.println(link);
      System.out.println();

      System.out.println("===== DUKEDB ======================================");
      System.out.println(dukedb);
      for (Link link : dukedb.getAllLinks())
        System.out.println(link);
    }
  }

  private String percent(int part, int total) {
    int p = (int) (((double) part * 1000) / (double) total);
    return "" + (p / 10.0);
  }

  private String getid(Record r) {
    for (Property p : idprops) {
      String v = r.getValue(p.getName());
      if (v == null)
        continue;

      return v;
    }

    throw new DukeException("No identity for record " + r);
  }
}
