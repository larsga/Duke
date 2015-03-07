
package no.priv.garshol.duke.cleaners;

import java.util.Map;
import java.util.HashMap;
import java.io.FileReader;
import java.io.IOException;

import no.priv.garshol.duke.Cleaner;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.utils.CSVReader;

// FIXME: we may also want an option to allow unmapped values to be
// returned as is (or even via the sub-cleaner)

/**
 * A cleaner which loads a mapping file in CSV format and maps values
 * according to that file.
 * @since 0.5
 */
public class MappingFileCleaner implements Cleaner {
  private Map<String, String> mapping;
  
  public String clean(String value) {
    String newvalue = mapping.get(value);
    if (newvalue == null)
      return value;
    return newvalue;
  }

  public void setMappingFile(String filename) {
    mapping = new HashMap();
    
    // FIXME: character encoding?
    try {
      CSVReader csv = new CSVReader(new FileReader(filename));

      String[] row = csv.next();
      while (row != null) {
        mapping.put(row[0], row[1]);
        row = csv.next();
      }
      
      csv.close();
    } catch (IOException e) {
      throw new DukeException("Error loading mapping file " + filename, e);
    }
  }  
}