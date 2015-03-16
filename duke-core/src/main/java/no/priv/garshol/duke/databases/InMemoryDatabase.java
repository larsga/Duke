
package no.priv.garshol.duke.databases;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.Property;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Configuration;

/**
 * Naïve in-memory store of records. Matches all records against all
 * other records.
 */
public class InMemoryDatabase implements Database {
  private Configuration config;
  private Map<String, Record> idindex;
  private Collection<Record> records;
  
  public InMemoryDatabase() {
    this.idindex = new HashMap();
    this.records = new ArrayList();
  }

  public void setConfiguration(Configuration config) {
    this.config = config;
  }

  public void setOverwrite(boolean overwrite) {
  }
  
  /**
   * Returns true iff the database is held entirely in memory, and
   * thus is not persistent.
   */
  public boolean isInMemory() {
    return true;
  }

  /**
   * Add the record to the index.
   */
  public void index(Record record) {
    for (Property p : config.getIdentityProperties()) {
      Collection<String> values = record.getValues(p.getName());
      if (values == null)
        continue;
      
      for (String id : values)
        idindex.put(id, record);
    }
    records.add(record);
  }

  /**
   * Look up record by identity.
   */
  public Record findRecordById(String id) {
    return idindex.get(id);
  }

  /**
   * Look up potentially matching records.
   */
  public Collection<Record> findCandidateMatches(Record record) {
    return records;
  }

  /**
   * Flushes all changes to disk. For in-memory databases this is a
   * no-op.
   */
  public void commit() {
  }
  
  /**
   * Stores state to disk and closes all open resources.
   */
  public void close() {
  }

  public String toString() {
    return "InMemoryDatabase";
  }
}
