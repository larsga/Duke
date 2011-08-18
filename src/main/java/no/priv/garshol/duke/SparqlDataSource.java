
package no.priv.garshol.duke;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.FileReader;
import java.io.IOException;

public class SparqlDataSource extends ColumnarDataSource {
  private static final int DEFAULT_PAGE_SIZE = 1000;
  private String endpoint;
  private String query;
  private int pagesize;
  /**
   * In triple mode we expect query results to be of the form:
   * (subject, property, value), whereas in normal mode we treat the
   * query as tabular (ie: one row per subject.
   */
  private boolean triple_mode;

  public SparqlDataSource() {
    this.columns = new HashMap();
    this.pagesize = DEFAULT_PAGE_SIZE;
    this.triple_mode = true;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public void setPageSize(int pagesize) {
    this.pagesize = pagesize;
  }

  public void setTripleMode(boolean triple_mode) {
    this.triple_mode = triple_mode;
  }

  public RecordIterator getRecords() {
    verifyProperty(endpoint, "endpoint");
    verifyProperty(query, "query");

    if (triple_mode)
      return new TripleModeIterator();
    else
      return new TabularIterator();
  }

  protected String getSourceName() {
    return "SPARQL";
  }

  // --- SparqlIterator

  abstract class SparqlIterator extends RecordIterator {
    protected int pageno;
    protected int pagerow;
    protected List<String> variables;
    protected List<String[]> page;
    
    public SparqlIterator() {
      fetchNextPage();
    }

    public boolean hasNext() {
      return pagerow < page.size();
    }
    
    public void remove() {
      throw new UnsupportedOperationException();
    }

    protected void fetchNextPage() {
      if (pagesize == 0 && pageno > 0) {
        // paging is turned off, and we've been asked to get the second page
        page = Collections.EMPTY_LIST;
        pagerow = 0;
        pageno++;
        return;
      }
        
      String thisquery = query;
      if (pagesize != 0) // paging is turned off
        thisquery += (" limit " + pagesize + " offset " + (pageno * pagesize));
      
      logger.debug("SPARQL query: " + thisquery);
      
      SparqlResult result = SparqlClient.execute(endpoint, thisquery);
      variables = result.getVariables();
      page = result.getRows();

      if (triple_mode && !page.isEmpty() && page.get(0).length != 3)
        throw new DukeConfigException("In triple mode SPARQL queries must " +
                                      "produce exactly three columns!");
      
      logger.debug("SPARQL result rows: " + page.size());
      
      pagerow = 0;
      pageno++;
    }

    protected void addValue(int valueix, Column col,
                            Map<String, Collection<String>> record) {
      if (col == null)
        return;
      
      String value = page.get(pagerow)[valueix];
      if (value == null)
        return;
      
      if (col.getCleaner() != null)
        value = col.getCleaner().clean(value);
      if (value == null || value.equals(""))
        return; // nothing here, move on
      
      String prop = col.getProperty();
      Collection<String> values = record.get(prop);
      if (values == null) {
        values = new ArrayList();
        record.put(prop, values);
      }
      values.add(value);
    }
  }

  class TripleModeIterator extends SparqlIterator {

    public Record next() {
      String resource = page.get(pagerow)[0];

      Column uricol = columns.get("?uri");
      Map<String, Collection<String>> record = new HashMap();
      record.put(uricol.getProperty(), Collections.singleton(resource));

      while (pagerow < page.size() && resource.equals(page.get(pagerow)[0])) {
        while (pagerow < page.size() && resource.equals(page.get(pagerow)[0])) {
          Column col = columns.get(page.get(pagerow)[1]);
          addValue(2, col, record);
          pagerow++;
        }

        // did we just step off this page?
        if (pagerow >= page.size())
          fetchNextPage();
      }

      return new RecordImpl(record);
    }
  }

  class TabularIterator extends SparqlIterator {

    public Record next() {
      Map<String, Collection<String>> record = new HashMap();

      for (int colix = 0; colix < variables.size(); colix++) {
        Column col = columns.get(variables.get(colix));
        addValue(colix, col, record);
      }

      pagerow++;
      // do we need to load the next page?
      if (pagerow >= page.size())
        fetchNextPage();
      
      return new RecordImpl(record);
    }
  }
}