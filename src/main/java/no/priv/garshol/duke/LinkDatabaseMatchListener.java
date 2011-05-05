
package no.priv.garshol.duke;

/**
 * Writes recorded matches to a LinkDatabase.
 */
public class LinkDatabaseMatchListener implements MatchListener {
  private Database database;
  private LinkDatabase linkdb;

  public LinkDatabaseMatchListener(Database database, LinkDatabase linkdb) {
    this.database = database;
    this.linkdb = linkdb;
  }
  
  public void matches(Record r1, Record r2, double confidence) {
    String id1 = getIdentity(r1);
    String id2 = getIdentity(r2);
    linkdb.assertLink(new Link(id1, id2, LinkStatus.INFERRED, LinkKind.SAME));
  }

  private String getIdentity(Record r) {
    for (Property p : database.getIdentityProperties())
      for (String v : r.getValues(p.getName()))
        return v;
    return null; // FIXME: this is really a problem
  }
  
}