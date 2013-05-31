
package no.priv.garshol.duke.utils;

import java.util.Collection;
import java.io.Writer;
import java.io.IOException;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.DukeException;

/**
 * Utility class for writing link files.
 * @since 1.1
 */
public class LinkFileWriter {
  private Writer out;
  private Collection<Property> idprops;

  public LinkFileWriter(Writer out, Configuration config) {
    this.out = out;
    this.idprops = config.getIdentityProperties();
  }

  public void write(Record r1, Record r2, boolean match) throws IOException {
    out.write("" + (match ? '+' : '-') + getid(r1) + ',' + getid(r2) + "\n");
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