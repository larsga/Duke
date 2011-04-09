
package no.priv.garshol.duke;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.io.File;
import java.io.IOException;

import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.queryParser.ParseException;

/**
 * Represents a ...
 */
public class Database {
  private Map<String, Property> properties;
  private Collection<Property> proplist; // duplicate to preserve order
  private Collection<Property> lookups; // subset of properties
  private String path;
  private IndexWriter iwriter;
  private Directory directory;
  private IndexSearcher searcher;
  private double threshold;
  private MatchListener listener;
  private Analyzer analyzer;

  public Database(String path, Collection<Property> props, double threshold)
    throws IOException, CorruptIndexException {
    this.path = path;
    this.proplist = props;
    this.properties = new HashMap(props.size());
    this.threshold = threshold;

    // register properties
    analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
    for (Property prop : props) {
      properties.put(prop.getName(), prop);
      prop.setParser(new QueryParser(Version.LUCENE_CURRENT, prop.getName(),
                                     analyzer));
    }

    // analyze properties to find lookup set
    findLookupProperties();
      
    // set up Lucene indexing
    openIndexes();
  }

  public void setMatchListener(MatchListener listener) {
    this.listener = listener;
  }
  
  public Collection<Property> getProperties() {
    return proplist;
  }

  public Collection<Property> getLookupProperties() {
    return lookups;
  }

  public Collection<Property> getIdentityProperties() {
    Collection<Property> ids = new ArrayList();
    for (Property p : getProperties())
      if (p.isIdProperty())
        ids.add(p);
    return ids;
  }

  public Property getPropertyByName(String name) {
    return properties.get(name);
  }

  public double getThreshold() {
    return threshold;
  }
  
  public void store(Record record) {
    // FIXME: implement!
    // persistently stores the record in raw form. this bit is tricky.
    // not sure what approach to use here. might use Lucene. might use
    // directory structure etc. we don't need this just yet, though,
    // so not doing this just yet.
  }

  /**
   * Add the record to the index.
   */
  public void index(Record record) throws CorruptIndexException, IOException {
    Document doc = new Document();

    for (String propname : record.getProperties()) {
      // FIXME: we assume just one value. not sure yet if this is safe.
      String value = record.getValue(propname);
      if (value == null || value.equals(""))
        continue; // FIXME: not sure if this is necessary
      
      Property prop = getPropertyByName(propname);
      Field.Index ix; // FIXME: could cache this. or get it from property
      if (prop.isIdProperty())
        ix = Field.Index.NO;
      else if (prop.isAnalyzedProperty())
        ix = Field.Index.ANALYZED;
      else
        ix = Field.Index.NOT_ANALYZED;

      doc.add(new Field(propname, value, Field.Store.YES, ix));
    }

    iwriter.addDocument(doc);
  }

  /**
   * Flushes all changes to disk.
   */
  public void commit() throws CorruptIndexException, IOException {
    if (searcher != null)
      searcher.close();
    iwriter.optimize();
    iwriter.commit();
    searcher = new IndexSearcher(directory, true);
  }

  /**
   * Look up potentially matching records for this property value.
   */
  public Collection<Record> lookup(Property prop, Collection<String> values)
    throws IOException, CorruptIndexException {    
    if (values == null || values.isEmpty())
      return Collections.EMPTY_SET;

    // true => read-only. must reopen every time to see latest changes to
    // index.
    QueryParser parser = prop.getParser();

    // FIXME: this algorithm is clean, but suboptimal.
    Collection<Record> matches = new ArrayList();
    for (String value : values) {
      String v = cleanLucene(value);
      if (v.length() == 0)
        continue; // possible if value consists only of Lucene characters
        
      try {                
        Query query = parser.parse(v);
        ScoreDoc[] hits = searcher.search(query, null, 50).scoreDocs;
        for (int ix = 0; ix < hits.length; ix++)
          matches.add(new DocumentRecord(searcher.doc(hits[ix].doc)));
      } catch (ParseException e) {
        System.out.println("Error on value '" + value + "', cleaned to '" +
                           v + "' for " + prop);
        throw new RuntimeException(e); // should be impossible
      }
    }
    
    return matches;
  }
  
  /**
   * Removes the record with the given ID from the index, if it's there.
   * If the record is not there this has no effect.
   */
  public void unindex(String id) {
  }

  /**
   * Removes all equality statements about this id, if any.
   */
  public void removeEqualities(String id) {
  }

  /**
   * Records the statement that the two records match.
   */
  public void registerMatch(Record r1, Record r2, double confidence) {
    // FIXME: actually record this information and make use of it.
    if (listener != null)
      listener.matches(r1, r2, confidence);
  }

  /**
   * Stores state to disk and closes all open resources.
   */
  public void close() throws CorruptIndexException, IOException {
    iwriter.close();
    directory.close();
    searcher.close();
  }

  // ----- INTERNALS

  private void findLookupProperties() {
    List<Property> candidates = new ArrayList();
    for (Property prop : properties.values())
      if (!prop.isIdProperty())
        candidates.add(prop);

    Collections.sort(candidates, new HighComparator());

    int ix;
    double prob = 0.5;    
    for (ix = 0; ix < candidates.size(); ix++) {
      prob = Utils.computeBayes(prob, candidates.get(ix).getHighProbability());
      if (prob >= threshold)
        break;
    }

    if (ix == candidates.size())
      throw new RuntimeException("Maximum possible probability is " + prob +
                                 ", which is below threshold (" + threshold +
                                 "), which means no duplicates will ever " +
                                 "be found");
    
    lookups = new ArrayList(candidates.subList(ix, candidates.size()));
  }

  private class HighComparator implements Comparator<Property> {
    public int compare(Property p1, Property p2) {
      if (p1.getHighProbability() < p2.getHighProbability())
        return -1;
      else if (p1.getHighProbability() == p2.getHighProbability())
        return 0;
      else
        return 1;
    }
  }

  private static String cleanLucene(String query) {
    char[] tmp = new char[query.length()];
    int count = 0;
    for (int ix = 0; ix < query.length(); ix++) {
      char ch = query.charAt(ix);
      if (ch != '*' && ch != '?' && ch != '!' && ch != '&' && ch != '(' &&
          ch != ')' && ch != '-' && ch != '+' && ch != ':' && ch != '"' &&
          ch != '[' && ch != ']')
        tmp[count++] = ch;
    }
    
    return new String(tmp, 0, count);
  }

  private void openIndexes() throws IOException, CorruptIndexException {
    directory = FSDirectory.open(new File(path));
    iwriter = new IndexWriter(directory, analyzer, true,
                              new IndexWriter.MaxFieldLength(25000));
    iwriter.commit(); // so that the searcher doesn't fail
  }
}