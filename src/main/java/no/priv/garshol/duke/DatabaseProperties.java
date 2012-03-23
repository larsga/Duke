
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

  private DatabaseImplementation dbtype =
    DatabaseImplementation.LUCENE_DATABASE;
  
  public void setDatabaseImplementation(String id) {
    this.dbtype = DatabaseImplementation.getbyid(id);
  }

  public DatabaseImplementation getDatabaseImplementation() {
    return dbtype;
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

  public enum DatabaseImplementation {
    LUCENE_DATABASE("lucene"),
    IN_MEMORY_DATABASE("in-memory");

    private String id;
    private DatabaseImplementation(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }

    public static DatabaseImplementation getbyid(String id) {
      if (id.equals(LUCENE_DATABASE.getId()))
        return LUCENE_DATABASE;
      else if (id.equals(IN_MEMORY_DATABASE.getId()))
        return IN_MEMORY_DATABASE;
      else
        throw new DukeConfigException("Unknown database type: '" + id + "'");
    }
  }
}