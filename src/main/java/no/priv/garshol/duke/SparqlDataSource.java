
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

public class SparqlDataSource implements DataSource {
  private static final int DEFAULT_PAGE_SIZE = 1000;
  private String endpoint;
  private String query;
  private int pagesize;
  private Map<String, Column> columns;

  public SparqlDataSource() {
    this.columns = new HashMap();
    this.pagesize = DEFAULT_PAGE_SIZE;
  }

  public void addColumn(Column column) {
    columns.put(column.getName(), column);
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

  public Iterator<Record> getRecords() {
    return new SparqlIterator();
  }

  // --- SparqlIterator

  class SparqlIterator implements Iterator<Record> {
    private int pageno;
    private int pagerow;
    private List<String[]> page;
    
    public SparqlIterator() {
      fetchNextPage();
    }

    public boolean hasNext() {
      return pagerow < page.size();
    }

    public Record next() {
      String resource = page.get(pagerow)[0];

      Column uricol = columns.get("?uri");
      Map<String, Collection<String>> record = new HashMap();
      record.put(uricol.getProperty(), Collections.singleton(resource));

      while (pagerow < page.size() && resource.equals(page.get(pagerow)[0])) {
        while (pagerow < page.size() && resource.equals(page.get(pagerow)[0])) {
          Column col = columns.get(page.get(pagerow)[1]);
          if (col == null) {
            pagerow++;
            continue;
          }

          String value = page.get(pagerow)[2];
          if (value == null)
            continue;
          if (col.getCleaner() != null)
            value = col.getCleaner().clean(value);
          if (value == null || value.equals(""))
            continue; // nothing here, move on
          
          String prop = col.getProperty();
          Collection<String> values = record.get(prop);
          if (values == null) {
            values = new ArrayList();
            record.put(prop, values);
          }
          values.add(value);

          pagerow++;
        }

        // did we just step off this page?
        if (pagerow >= page.size())
          fetchNextPage();
      }

      return new RecordImpl(record);
    }
    
    public void remove() {
      throw new UnsupportedOperationException();
    }

    private void fetchNextPage() {
      String thisquery = query + " limit " + pagesize +
                                 " offset " + (pageno * pagesize);
      
      System.out.println("query: " + thisquery);
      page = SparqlClient.execute(endpoint, thisquery);
      pagerow = 0;
      pageno++;
    }
  }
}