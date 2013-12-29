
package no.priv.garshol.duke;

/**
 * A class representing configurable properties on the record database.
 */
public class DatabaseProperties {
  // Deichman case:
  //  100 = 2 minutes
  //  1000 = 10 minutes
  //  10000 = 50 minutes
  private int max_search_hits = 10000000;

  private float min_relevance = 0.0f;

  private String dbklass = "no.priv.garshol.duke.LuceneDatabase";
  
  public void setDatabaseImplementation(String klass) {
    if (klass.equals("lucene"))
      klass = "no.priv.garshol.duke.LuceneDatabase";
    else if (klass.equals("in-memory"))
      klass = "no.priv.garshol.duke.InMemoryDatabase";
    else if (klass.equals("key-value"))
      klass = "no.priv.garshol.duke.KeyValueDatabase";
    
    this.dbklass = klass;
  }

  public String getDatabaseImplementation() {
    return dbklass;
  }

  public void setMinRelevance(float min) {
    this.min_relevance = min;
  }

  public float getMinRelevance() {
    return min_relevance;
  }

  public void setMaxSearchHits(int max) {
    this.max_search_hits = max;
  }

  public int getMaxSearchHits() {
    return max_search_hits;
  }
}