

package no.priv.garshol.duke.cleaners;

import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import no.priv.garshol.duke.Cleaner;
import no.priv.garshol.duke.utils.StringUtils;

/**
 * <b>Experimental</b> cleaner for person names, which understands
 * about abbreviations like "joe" for "joseph", etc.
 */
public class PersonNameCleaner implements Cleaner {
  private LowerCaseNormalizeCleaner sub;
  private Map<String, String> mapping;

  public PersonNameCleaner() {
    this.sub = new LowerCaseNormalizeCleaner();

    // load token translation mapping (FIXME: move to static init?)
    try {
      this.mapping = loadMapping();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String clean(String value) {
    // do basic cleaning
    value = sub.clean(value);
    if (value == null || value.equals(""))
      return value;

    // tokenize, then map tokens, then rejoin
    String[] tokens = StringUtils.split(value);
    for (int ix = 0; ix < tokens.length; ix++) {
      String mapsto = mapping.get(tokens[ix]);
      if (mapsto != null)
        tokens[ix] = mapsto;
    }
    
    return StringUtils.join(tokens);
  }

  private Map<String, String> loadMapping() throws IOException {
    String mapfile = "no/priv/garshol/duke/name-mappings.txt";
    
    Map<String, String> mapping = new HashMap();
    ClassLoader cloader = Thread.currentThread().getContextClassLoader();
    InputStream istream = cloader.getResourceAsStream(mapfile);
    InputStreamReader reader = new InputStreamReader(istream, "utf-8");
    BufferedReader in = new BufferedReader(reader);

    String line = in.readLine();
    while (line != null) {
      int pos = line.indexOf(',');
      mapping.put(line.substring(0, pos), line.substring(pos + 1));
      line = in.readLine();
    }

    in.close();
    return mapping;
  }
}