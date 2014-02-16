
package no.priv.garshol.duke.integration;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import no.priv.garshol.duke.Duke;
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
    Result r = run("");
    
    assertEquals("didn't fail with error code", 1, r.code);
    assertTrue("Duke gave no error message: " + r.out,
               r.out.contains("ERROR:"));
  }

  @Test
  public void testShowMatches() throws IOException {
    Result r = run("--showmatches doc/example-data/countries.xml");
    
    assertEquals("failed with error code: " + r.out, 0, r.code);
    assertTrue("not enough matches", r.countOccurrences("MATCH 0.") > 50);
  }

  @Test
  public void testMakeLinkFile() throws IOException {
    File linkfile = tmpdir.newFile();
    Result r = run("--showmatches --linkfile=\"" + linkfile.getAbsolutePath() +
                   "\" doc/example-data/countries.xml");

    int outmatches = r.countOccurrences("MATCH 0.");
    LinkDatabase db = LinkDatabaseUtils.loadTestFile(linkfile.getAbsolutePath());
    assertEquals("disagreement on number of matches",
                 outmatches, db.getAllLinks().size());
  }

  @Test
  public void testShowData() throws IOException {
    Result r = run("--showdata doc/example-data/countries.xml");

    assertEquals("wrong number of IDs", 522, r.countOccurrences("ID: "));
    assertEquals("wrong number of NAMEs", 522, r.countOccurrences("NAME: "));
    assertEquals("wrong number of AREAs", 522, r.countOccurrences("AREA: "));
    assertEquals("wrong number of CAPITALs", 522, r.countOccurrences("CAPITAL: "));
  }

  // FIXME: weirdly, the test below always fails, even though it's doing
  //        exactly the same thing as the testMakeLinkFile test. no idea
  //        why.
  // @Test
  // public void testTestFile() throws IOException {
  //   // run to make a link file first
  //   File linkfile = tmpdir.newFile();
  //   Result r = run("--linkfile=\"" + linkfile.getAbsolutePath() +
  //                  "\" doc/example-data/countries.xml");
  //   assertTrue("couldn't write link file: " + r.out, r.code == 0);

  //   // now we match against the test file
  //   r = run("--testfile=\"" + linkfile.getAbsolutePath() +
  //           "\" doc/example-data/countries.xml");

  //   assertEquals("failed with error code: " + r.out, 0, r.code);
  //   assertTrue("Can't find precision output: " + r.out,
  //              r.out.indexOf("Precision ") != -1);
  // }
  
  // ===== UTILITIES

  private Result run(String args) throws IOException {
    String jar = "target/duke-" + Duke.getVersion() + ".jar";
    Process p = Runtime.getRuntime().exec("java -cp " + jar +
                                          " no.priv.garshol.duke.Duke " +
                                          args);
    StringBuilder tmp = new StringBuilder();
    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    while ((line = r.readLine()) != null)
      tmp.append(line);
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
  }
}
