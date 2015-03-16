
package no.priv.garshol.duke.datasources;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import no.priv.garshol.duke.ConfigWriter;
import no.priv.garshol.duke.DukeException;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;

/**
 * DataSource which reads a collection of JSON as records.
 *
 * <p>WARNING: this datasource isn't nesting-aware yet: if two fields
 * share the same name while being at different nesting levels, they
 * will be treated the same way.
 */
public class JsonDataSource extends ColumnarDataSource {
  private String file;

  public JsonDataSource() {
    super();
  }

  public String getInputFile() {
    return file;
  }

  public void setInputFile(String file) {
    this.file = file;
  }

  public RecordIterator getRecordsFromString(String s) {
    try {
      JsonFactory jsonFactory = new JsonFactory();
      return new JsonIterator(jsonFactory.createParser(new StringReader(s)));
    } catch (IOException e) {
      throw new DukeException(e);
    }
  }

  public RecordIterator getRecordsFromStream(InputStream is) {
    JsonFactory jsonFactory = new JsonFactory();
    try {
      JsonParser jp = jsonFactory.createParser(is);
      return new JsonIterator(jp);
    } catch (IOException e) {
      throw new DukeException(e);
    }
  }

  public RecordIterator getRecords() {
    try {
      return getRecordsFromStream(new FileInputStream(file));
    } catch (IOException e) {
      throw new DukeException("Error reading JSON from " + file, e);
    }
  }

  @Override
  public void writeConfig(ConfigWriter cw) {
    final String name = "json";
    cw.writeStartElement(name, null);

    // Write columns
    writeColumnsConfig(cw);

    cw.writeEndElement(name);
  }

  public String getSourceName() {
    return "JSON";
  }

  protected class JsonIterator extends RecordIterator {
    private JsonParser parser;
    private RecordBuilder builder;
    private Map<String, Set<Column>> cols = new HashMap();

    public JsonIterator(JsonParser jp) {
      this.parser = jp;
      this.builder = new RecordBuilder(JsonDataSource.this);
      for (Column col : getColumns()) {
        if (cols.containsKey(col.getName()))
          cols.get(col.getName()).add(col);
        else
          cols.put(col.getName(), new HashSet(Arrays.asList(col)));
      }
    }

    //buffer for a token, used to avoid hasNext() to impact the json parsing
    private JsonToken tokenFromHasNext;

    @Override
    public boolean hasNext() {
      try {
        tokenFromHasNext = parser.nextToken();
        return (tokenFromHasNext == JsonToken.START_OBJECT);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return false;
    }

    @Override
    public Record next() {
      builder.newRecord();
      Set<Column> currentCols = null;
      try {
        JsonToken currentToken;
        int nestingLevel = -1;
        do {
          if (tokenFromHasNext == null) {
            currentToken = parser.nextToken();
          } else {
            currentToken = tokenFromHasNext;
            tokenFromHasNext = null;
          }
          
          if (currentToken == JsonToken.START_OBJECT) {
            nestingLevel++;
          } else if (currentToken == JsonToken.END_OBJECT) {
            nestingLevel--;
          } else if (currentToken == JsonToken.FIELD_NAME) {
            currentCols = cols.get(parser.getCurrentName());
          } else if (currentToken == JsonToken.END_ARRAY) {
          } else if (currentCols != null &&
                     currentToken != JsonToken.START_ARRAY &&
                     currentToken != JsonToken.END_ARRAY)
            for (Column currentCol : currentCols)
              builder.addValue(currentCol, parser.getText());

        } while (currentToken != JsonToken.END_OBJECT || nestingLevel != -1);
      } catch (IOException e) {
        throw new DukeException(e);
      }
      return builder.getRecord();
    }
  }
}
