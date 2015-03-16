
package no.priv.garshol.duke.integration;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import no.priv.garshol.duke.Duke;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.LinkDatabase;
import no.priv.garshol.duke.utils.LinkDatabaseUtils;

/**
 * Duke integration tests, testing the command-line tools.
 */
public class IT {
  @Rule
  public TemporaryFolder tmpdir = new TemporaryFolder();

  @Test
  public void testFailWithNoArguments() throws IOException {
    Result r = duke("");

    assertEquals("didn't fail with error code", 1, r.code);
    assertTrue("Duke gave no error message: " + r.out,
               r.out.contains("ERROR:"));
  }

  @Test
  public void testShowMatches() throws IOException {
    Result r = duke("--showmatches doc/example-data/countries.xml");

    assertEquals("failed with error code: " + r.out, 0, r.code);
    assertTrue("not enough matches", r.countOccurrences("MATCH 0.") > 50);
  }

  @Test
  public void testMakeLinkFile() throws IOException {
    File linkfile = tmpdir.newFile();
    Result r = duke("--showmatches --linkfile=\"" + linkfile.getAbsolutePath() +
                   "\" doc/example-data/countries.xml");

    int outmatches = r.countOccurrences("MATCH 0.");
    LinkDatabase db = LinkDatabaseUtils.loadTestFile(linkfile.getAbsolutePath());
    assertEquals("disagreement on number of matches",
                 outmatches, db.getAllLinks().size());
  }

  @Test
  public void testShowData() throws IOException {
    Result r = duke("--showdata doc/example-data/countries.xml");

    assertEquals("wrong number of IDs", 522, r.countOccurrences("ID: "));
    assertEquals("wrong number of NAMEs", 522, r.countOccurrences("NAME: "));
    assertEquals("wrong number of AREAs", 522, r.countOccurrences("AREA: "));
    assertEquals("wrong number of CAPITALs", 522, r.countOccurrences("CAPITAL: "));
  }

  // FIXME: weirdly, the test below always fails, even though it's doing
  //        exactly the same thing as the testMakeLinkFile test. no idea
  //        why.
  @Test @Ignore
  public void testTestFile() throws IOException {
    // run to make a link file first
    File linkfile = tmpdir.newFile();
    Result r = duke("--linkfile=\"" + linkfile.getAbsolutePath() +
                   "\" doc/example-data/countries.xml");
    assertTrue("couldn't write link file: " + r.out, r.code == 0);

    // now we match against the test file
    r = duke("--testfile=\"" + linkfile.getAbsolutePath() +
            "\" doc/example-data/countries.xml");

    assertEquals("failed with error code: " + r.out, 0, r.code);
    assertTrue("Can't find precision output: " + r.out,
               r.contains("Precision "));
  }

  @Test
  public void testGenetic() throws IOException {
    // first produce a configuration with the genetic algorithm
    File cfgfile = tmpdir.newFile();
    Result r = genetic("--testfile=doc/example-data/countries-test.txt --generations=2 --output=" + cfgfile.getAbsolutePath() + " doc/example-data/countries.xml");
    assertEquals("failed with error code: " + r.out, 0, r.code);
    assertEquals("Didn't run for 2 generations", 2,
                 r.countOccurrences("BEST: "));
    float bestscore = r.floatAfterLast("BEST: ");
    assertTrue("couldn't find a good solution",
               bestscore > 0.95);

    // then run Duke with the configuration we made
    r = duke("--testfile=doc/example-data/countries-test.txt --singlematch " +
             cfgfile.getAbsolutePath());
    assertEquals("failed with error code: " + r.out, 0, r.code);
    float realscore = r.floatAfterLast("f-number ");

    // FIXME: for some reason, the real score sometimes differs from the
    //        score found by the genetic algorithm. WHY???
    // assertEquals("real score different from expected",
    //              bestscore, realscore, 0.01);
  }

  @Test
  public void testGeneticActive() throws IOException {
    // first produce a configuration with active learning
    File cfgfile = tmpdir.newFile();
    Result r = genetic("--generations=4 --output=" + cfgfile.getAbsolutePath() + " --population=20 --testfile=doc/example-data/countries-test.txt --scientific doc/example-data/countries.xml");
    assertEquals("failed with error code: " + r.out, 0, r.code);
    assertEquals("Didn't run for 4 generations", 4,
                 r.countOccurrences("===== GENERATION "));
    float bestscore = r.floatAfterLast("ACTUAL BEST: ");
    assertTrue("couldn't find a good solution",
               bestscore > 0.85);

    // then run Duke with the configuration we made
    r = duke("--testfile=doc/example-data/countries-test.txt --singlematch " +
             cfgfile.getAbsolutePath());
    assertEquals("failed with error code: " + r.out, 0, r.code);
    float realscore = r.floatAfterLast("f-number ");
    // FIXME: figure out why it's sometimes different
    // assertEquals("real score different from expected",
    //              bestscore, realscore, 0.01);
  }

  @Test
  public void testDebugCompare() throws IOException {
    Result r = runjava("DebugCompare", "--reindex doc/example-data/countries.xml http://dbpedia.org/resource/Andorra 7021");
    assertEquals("failed with error code: " + r.out, 0, r.code);
    assertTrue("didn't reindex", r.contains("Reindexing"));
    assertTrue("no mention of NAME", r.contains("NAME"));
    assertTrue("no mention of AREA", r.contains("AREA"));
    assertTrue("no mention of CAPITAL", r.contains("CAPITAL"));
    assertTrue("doesn't think Andorra is equal to itself",
               r.floatAfterLast("Overall: ") > 0.9);
  }

  @Test @Ignore // Travis does not accept tests running more than 10 mins
  public void testGeneticLong() throws IOException {
    Result r = genetic("--testfile=doc/example-data/countries-test.txt doc/example-data/countries.xml");
    assertEquals("failed with error code: " + r.out, 0, r.code);
    assertEquals("Didn't run for 100 generations", 100,
                 r.countOccurrences("BEST: "));
    assertTrue("couldn't find a good solution",
               r.floatAfterLast("BEST: ") > 0.95);
  }

  // ===== UTILITIES

  private Result duke(String args) throws IOException {
    return runjava("Duke", args);
  }

  private Result genetic(String args) throws IOException {
    return runjava("genetic.Driver", args);
  }

  private Result runjava(String klass, String args) throws IOException {
    String jar = "target/duke-" + Duke.getVersion() + ".jar";
    String cmd = "java -cp " + jar + " no.priv.garshol.duke." + klass +
                  " " + args;
    return run(cmd);
  }

  private Result run(String cmd) throws IOException {
    Process p = Runtime.getRuntime().exec(cmd);

    StringBuilder tmp = new StringBuilder();
    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    while ((line = r.readLine()) != null)
      tmp.append(line + " ");
    r.close();

    r = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    while ((line = r.readLine()) != null)
      tmp.append(line + " ");
    r.close();

    try {
      p.waitFor(); // we wait for process to exit
    } catch (InterruptedException e) {
    }

    return new Result(tmp.toString(), p.exitValue());
  }

  private static class Result {
    public String out;
    public int code;

    public Result(String out, int code) {
      this.out = out;
      this.code = code;
    }

    public int countOccurrences(String sub) {
      int pos = 0;
      int count = 0;
      while (true) {
        pos = out.indexOf(sub, pos);
        if (pos == -1)
          return count;
        count++;
        pos += sub.length();
      }
    }

    public boolean contains(String sub) {
      return out.indexOf(sub) != -1;
    }

    public float floatAfterLast(String sub) {
      // first, scan to find last occurrence of 'sub'
      int pos = 0;
      while (true) {
        int postpos = out.indexOf(sub, pos);
        if (postpos == -1)
          break;
        pos = postpos + sub.length();
      }

      // then parse the float
      int ix;
      for (ix = pos; ix < out.length(); ix++) {
        char ch = out.charAt(ix);
        if (ch != '.' && (ch < '0' || ch > '9'))
          break;
      }

      // finally
      if (pos == ix)
        throw new DukeException("Couldn't find float in " + out);
      return Float.valueOf(out.substring(pos, ix));
    }

    public String toString() {
      return "[Run result, code " + code + ", output:\n" + out + "\n]";
    }
  }
}
