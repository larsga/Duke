
package no.priv.garshol.duke.datasources;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import no.priv.garshol.duke.ConfigWriter;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordImpl;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.StatementHandler;
import no.priv.garshol.duke.utils.NTriplesParser;

/**
 * A data source which can read RDF data from NTriples files. By
 * default it loads the entire data set into memory, in order to build
 * complete records. However, if the file is sorted you can call
 * setIncrementalMode(true) to avoid this.
 */
public class NTriplesDataSource extends ColumnarDataSource {
  private String file;
  private boolean incremental = false;
  private Collection<String> types;
  private Reader directreader;

  public NTriplesDataSource() {
    super();
    this.types = new HashSet();
  }

  public void setInputFile(String file) {
    this.file = file;
  }

  public void setAcceptTypes(String types) {
    // FIXME: accept more than one
    this.types.add(types);
  }
  
  // this is used only for testing
  public void setReader(Reader reader) {
    this.directreader = reader;
  }

  public void setIncrementalMode(boolean incremental) {
    this.incremental = incremental;
  }

  public RecordIterator getRecords() {
    if (directreader == null)
      verifyProperty(file, "input-file");
    
    try {
      Reader reader = directreader;
      if (reader == null)
        reader = new InputStreamReader(new FileInputStream(file), "utf-8");
      if (!incremental) {
        // non-incremental mode: everything gets built in memory
        RecordHandler handler = new RecordHandler(types);
        NTriplesParser.parse(reader, handler);
        handler.filterByTypes();
        Iterator it = handler.getRecords().values().iterator();
        return new DefaultRecordIterator(it);
      } else
        // incremental mode: we load records one at a time, as we iterate
        // over them.
        return new IncrementalRecordIterator(reader);

    } catch (IOException e) {
      throw new DukeException(e);
    }
  }

  protected String getSourceName() {
    return "NTriples";
  }

  // common utility method for adding a statement to a record
  private void addStatement(RecordImpl record,
                            String subject,
                            String property,
                            String object) {
    Collection<Column> cols = columns.get(property);
    if (cols == null) {
      if (property.equals(RDF_TYPE) && !types.isEmpty())
        addValue(record, subject, property, object);
      return;
    }
    
    for (Column col : cols) {
      String cleaned = object;
      if (col.getCleaner() != null)
        cleaned = col.getCleaner().clean(object);
      if (cleaned != null && !cleaned.equals(""))
        addValue(record, subject, col.getProperty(), cleaned);
    }
  }

  private void addValue(RecordImpl record, String subject,
                        String property, String object) {
    if (record.isEmpty())
      for (Column idcol : columns.get("?uri"))
        record.addValue(idcol.getProperty(), subject);

    record.addValue(property, object);      
  }

  private boolean filterbytype(Record record) {
    if (types.isEmpty()) // there is no filtering
      return true;
      
    boolean found = false;
    for (String value : record.getValues(RDF_TYPE))
      if (types.contains(value))
        return true;
    return false;
  }

  @Override
  public void writeConfig(ConfigWriter cw) {
    final String name = "ntriples";
    cw.writeStartElement(name, null);

    // Write columns
    writeColumnsConfig(cw);

    cw.writeEndElement(name);
  }

  // ----- non-incremental handler

  private static final String RDF_TYPE =
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
  
  class RecordHandler implements StatementHandler {
    private Map<String, RecordImpl> records;
    private Collection<String> types;

    public RecordHandler(Collection<String> types) {
      this.records = new HashMap();
      this.types = types;
    }

    public void filterByTypes() {
      // this is fairly ugly. if types has values we add an extra property
      // RDF_TYPE to records during build, then filter out records of the
      // wrong types here. finally, we strip away the RDF_TYPE property here.
      
      if (types.isEmpty())
        return;
      
      for (String uri : new ArrayList<String>(records.keySet())) {
        RecordImpl r = records.get(uri);
        if (!filterbytype(r))
          records.remove(uri);
        else
          r.remove(RDF_TYPE);
      }
    }

    public Map<String, RecordImpl> getRecords() {
      return records;
    }

    // FIXME: refactor this so that we share code with addStatement()
    public void statement(String subject, String property, String object,
                          boolean literal) {
      RecordImpl record = records.get(subject);
      if (record == null) {
        record = new RecordImpl();
        records.put(subject, record);
      }
      addStatement(record, subject, property, object);
    }
  }

  // --- default mode

  public class DefaultRecordIterator extends RecordIterator {
    private Record next;
    private Iterator<Record> it;
    
    public DefaultRecordIterator(Iterator<Record> it) {
      this.it = it;
      findNext();
    }
    
    public boolean hasNext() {
      return next != null;
    }
    
    public Record next() {
      if (next == null)
        return it.next(); // will throw exception

      Record tmp = next;
      findNext();
      return tmp;
    }

    private void findNext() {
      while (it.hasNext()) {
        next = it.next();
        if (!next.getProperties().isEmpty())
          return; // we found it!
      }
      next = null;
    }
  }

  // --- incremental mode

  class IncrementalRecordIterator extends RecordIterator
    implements StatementHandler {

    private BufferedReader reader;
    private NTriplesParser parser;
    private Record nextrecord;
    private String subject;
    private String property;
    private String object;
    
    public IncrementalRecordIterator(Reader input) {
      this.reader = new BufferedReader(input);
      this.parser = new NTriplesParser(this);
      parseNextLine();
      findNextRecord();
    }

    public boolean hasNext() {
      return nextrecord != null;
    }
    
    public Record next() {
      Record record = nextrecord;
      findNextRecord();
      return record;
    }

    // find the next record that's of an acceptable type
    private void findNextRecord() {
      do {
        nextrecord = parseRecord();
      } while (nextrecord != null && !filterbytype(nextrecord));
    }

    private void parseNextLine() {
      // blanking out, so we can see whether we receive anything in
      // each line we parse.
      subject = null;

      String nextline;
      while (subject == null) {
        try {
          nextline = reader.readLine();
        } catch (IOException e) {
          throw new DukeException(e);
        }
        
        if (nextline == null)
          return; // we're finished, and there is no next record

        parser.parseLine(nextline);
        // we've now received a callback setting 'subject' if there
        // was a statement in this line.
      }
    }

    // this finds the next record in the data stream
    private Record parseRecord() {
      RecordImpl record = new RecordImpl();
      String current = subject;

      // we've stored the first statement about the next resource, so we
      // need to process that before we move on to read anything
      
      while (current != null && current.equals(subject)) {
        addStatement(record, subject, property, object);
        parseNextLine();
        // we have now received a callback to statement() with the
        // next statement
      }

      // ok, subject is now either null (we're finished), or different from
      // current, because we've started on the next resource
      if (current == null)
        return null;
      return record;
    }

    public void statement(String subject, String property, String object,
                          boolean literal) {
      this.subject = subject;
      this.property = property;
      this.object = object;
    }
  }
}