
package no.priv.garshol.duke.utils;

import java.io.File;
import java.io.IOException;

import no.priv.garshol.duke.DukeException;

public class Utils {

  /**
   * Combines two probabilities using Bayes' theorem. This is the
   * approach known as "naive Bayes", very well explained here:
   * http://www.paulgraham.com/naivebayes.html
   */
  public static double computeBayes(double prob1, double prob2) {
    return (prob1 * prob2) /
      ((prob1 * prob2) + ((1.0 - prob1) * (1.0 - prob2)));
  }

  /**
   * Returns true iff we are running on Windows. Used to detect
   * whether it's safe to use Lucene's NIOFSDirectory. It's slow on
   * Windows due to a Java bug.
   */
  public static boolean isWindowsOS() {
    return System.getProperty("os.name").startsWith("Windows");
  }
  
  /**
   * Creates a temporary folder using the given prefix to generate its name.
   * @param prefix the prefix string to be used in generating the directory's name; may be <i>null</i>
   * @return the <code>File</code> to the newly created folder
   * @throws IOException
   */
  public static File createTempDirectory(String prefix) {
	File temp = null;
	
	try {
	  temp = File.createTempFile(prefix != null ? prefix : "temp", Long.toString(System.nanoTime()));
	
	  if (!(temp.delete())) {
	    throw new IOException("Could not delete temp file: "
		  + temp.getAbsolutePath());
	  }
	
	  if (!(temp.mkdir())) {
	    throw new IOException("Could not create temp directory: "
	      + temp.getAbsolutePath());
	  }
	} catch (IOException e) {
	  throw new DukeException("Unable to create temporary directory with prefix " + prefix, e);
	}
	
	return temp;
  }
}
