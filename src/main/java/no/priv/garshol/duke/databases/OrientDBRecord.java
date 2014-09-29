package no.priv.garshol.duke.databases;

import com.orientechnologies.orient.core.record.impl.ODocument;
import no.priv.garshol.duke.Record;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by nhamblet.
 */
public class OrientDBRecord implements Record {

    private ODocument doc;

    public OrientDBRecord(ODocument doc) {
        this.doc = doc;
    }

    public Collection<String> getProperties() {
        return Arrays.asList(this.doc.fieldNames());
    }

    public Collection<String> getValues(String prop) {
        if (doc.field(prop) != null) {
            return Arrays.asList(doc.field(prop).toString());
        } else {
            return Arrays.asList();
        }
    }

    public String getValue(String prop) {
        return doc.field(prop).toString();
    }

    public void merge(Record other) {
        throw new UnsupportedOperationException("Can't merge OrientDBRecords");
    }

    public String toString() {
        return "[OrientDBRecord(" + doc.toString() + ")]";
    }
}
