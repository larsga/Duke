
package no.priv.garshol.duke.integration;

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

/**
 * Duke integration tests, testing the command-line tools.
 */
public class IT {

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
  
  // ===== UTILITIES

  private Result run(String args) throws IOException {
    Process p = Runtime.getRuntime().exec("java -cp target/duke*.jar no.priv.garshol.duke.Duke " +
                                          args);
    StringBuilder tmp = new StringBuilder();
    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    while ((line = r.readLine()) != null)
      tmp.append(line);
    r.close();

    r = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    while ((line = r.readLine()) != null)
      tmp.append(line);
    r.close();

    p.waitFor(); // we wait for process to exit

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
