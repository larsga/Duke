package no.priv.garshol.duke.databases;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Database;
import no.priv.garshol.duke.Record;

import java.util.*;

/**
 * Created by nhamblet.
 */
public class OrientDBDatabase implements Database {
    private Configuration config;

    private ODatabaseDocumentTx db;
    private String db_uri;
    private String db_username;
    private String db_password;
    private OSQLSynchQuery<ODocument> db_candidate_query;

    public OrientDBDatabase() {
    }

    public void setConfiguration(Configuration config) {
        this.config = config;

        try {
            db = new ODatabaseDocumentTx(db_uri);
            db.open(db_username, db_password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDbUri(String db_uri) {
        this.db_uri = db_uri;
    }

    public void setDbUsername(String db_username) {
        this.db_username = db_username;
    }

    public void setDbPassword(String db_password) {
        this.db_password = db_password;
    }

    public void setDbCandidateQuery(String db_candidate_query) {
        this.db_candidate_query = new OSQLSynchQuery<ODocument>(db_candidate_query);
    }

    public boolean isInMemory() {
        return false;
    }

    public void commit() {
        if (db != null) {
            try {
                db.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Record findRecordById(String id) {
        throw new UnsupportedOperationException("Can't lookup record by id in OrientDBDatabase");
    }

    public void setOverwrite(boolean overwrite) {
    }

    public void index(Record record) {
        throw new UnsupportedOperationException("Can't index record in OrientDBDatabase");
    }

    public Collection<Record> findCandidateMatches(Record record) {
        List<ODocument> oResult = db.command(db_candidate_query).execute(toQueryParams(record));
        Collection<Record> result = new ArrayList<Record>();
        for (ODocument doc : oResult) {
            result.add(new OrientDBRecord(doc));
        }
        return result;
    }

    private Map<Object, Object> toQueryParams(Record record) {
        Map<Object, Object> params = new HashMap<Object, Object>();
        for (String property : record.getProperties()) {
            params.put(property, record.getValue(property));
        }
        return params;
    }

    public void close() {
        db.close();
    }

    public String toString() {
        return "OrientDBDatabase";
    }
}
