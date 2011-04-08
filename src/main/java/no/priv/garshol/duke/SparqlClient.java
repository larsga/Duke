
package no.priv.garshol.duke;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.net.HttpURLConnection;

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

public class SparqlClient {
  private static final String SPARQL_NS =
    "http://www.w3.org/2005/sparql-results#";

  public static List<String[]> execute(String endpoint, String query) {
    try {
      query = urlencode(query);
      String url = endpoint + "?query=" + query;

      ResultHandler handler = new ResultHandler();
      XMLReader parser = XMLReaderFactory.createXMLReader();
      parser.setContentHandler(handler);
      parser.parse(getResponse(url));
      return handler.getResults();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }
  }

  private static InputSource getResponse(String url) throws IOException {
    URL urlobj = new URL(url);
    URLConnection conn = urlobj.openConnection();
    conn.setRequestProperty("Accept", "application/sparql-results+xml");
    return new InputSource(conn.getInputStream());
  }

  private static String urlencode(String url) throws IOException {
    // return url.replace(" ", "+");
    return URLEncoder.encode(url, "utf-8");
  }

  private static class ResultHandler extends DefaultHandler {
    private List<String[]> result;
    private String[] currentRow;

    private Map<String, Integer> columnIndexes;
    private int colix;
    private String curvar;
    
    private Set<String> keepers;
    private boolean keep;
    private StringBuffer content;

    private ResultHandler() {
      this.result = new ArrayList();

      this.columnIndexes = new HashMap();
      this.colix = 0;
      
      this.keepers = new HashSet();
      this.content = new StringBuffer();
      keepers.add("uri");
      keepers.add("literal");
    }

    public List<String[]> getResults() {
      return result;
    }

    public void	startElement(String uri, String localName, String qName,
                             Attributes attributes) {
      if (!SPARQL_NS.equals(uri))
        return; // not SPARQL namespace, so we don't know what it is
      
      if (localName.equals("variable")) {
        String var = attributes.getValue("name");
        columnIndexes.put(var, colix++);

      } else if (localName.equals("result"))
        currentRow = new String[3];

      else if (localName.equals("binding"))
        curvar = attributes.getValue("name");

      else if (keepers.contains(localName)) {
        keep = true;
        content.setLength(0);
      }
    }

    public void characters(char[] ch, int start, int length) {
      if (keep)
        content.append(ch, start, length);
    }

    public void endElement(String uri, String localName, String qName) {
      if (!SPARQL_NS.equals(uri))
        return; // not SPARQL namespace, so we don't know what it is

      if (localName.equals("binding")) {
        int ix = columnIndexes.get(curvar);
        currentRow[ix] = content.toString();
      } else if (localName.equals("result"))
        result.add(currentRow);
      else if (keepers.contains(localName))
        keep = false;
    }
  }  
  
}