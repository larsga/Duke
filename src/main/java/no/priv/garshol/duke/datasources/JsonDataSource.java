package no.priv.garshol.duke.datasources;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;

import java.io.*;
import java.util.*;

/**
 * DataSource which reads a collection of JSON as records.
 * WARNING : this datasource isn't nesting-aware yet : if two fields share the same name while being at different nesting levels, they will be treated the same way.
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
            InputStream stream = new ByteArrayInputStream(s.getBytes("UTF-8"));
            return this.getRecordsFromStream(stream);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new EmptyRecordIterator();
        }
    }

    public RecordIterator getRecordsFromStream(InputStream is){
        JsonFactory jsonFactory = new JsonFactory();
        try {
            JsonParser jp = jsonFactory.createParser(is);
            return new JsonIterator(jp);
        } catch (IOException e) {
            e.printStackTrace();
            return new EmptyRecordIterator();
        }
    }

    public RecordIterator getRecords() {
        try {
            getRecordsFromStream(new FileInputStream(this.file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getSourceName() {
        return "JSON";
    }

    /**
     * empty iterator returned when data couldn't be parsed
     * should be
     */
    protected class EmptyRecordIterator extends RecordIterator {
        public boolean hasNext() { return false;}

        public Record next() {
            return null;
        }

    }

    protected class JsonIterator extends RecordIterator {
        private JsonParser parser;
        private RecordBuilder builder;
        private Map<String, Set<Column>> cols = new HashMap<String, Set<Column>>();

        public JsonIterator(JsonParser jp) {
            this.parser = jp;
            this.builder = new RecordBuilder(JsonDataSource.this);
            for (Column col : getColumns()) {
                if (this.cols.containsKey(col.getName()))
                    this.cols.get(col.getName()).add(col);
                else this.cols.put(col.getName(), new HashSet<Column>(Arrays.asList(col)));
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
                    } else{
                        currentToken = tokenFromHasNext;
                        tokenFromHasNext = null;
                    }
                    if (currentToken == JsonToken.START_OBJECT) {
                        nestingLevel++;
                    } else if (currentToken == JsonToken.END_OBJECT) {
                        nestingLevel--;
                    } else if (currentToken == JsonToken.FIELD_NAME) {
                        currentCols = cols.get(parser.getCurrentName());
                    }else if (currentToken == JsonToken.END_ARRAY){
                    } else if (currentCols != null && currentToken != JsonToken.START_ARRAY && currentToken != JsonToken.END_ARRAY) {
                        for (Column currentCol : currentCols) {
                            builder.addValue(currentCol, parser.getText());
                        }
                    }
                } while (currentToken != JsonToken.END_OBJECT || nestingLevel != -1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return builder.getRecord();
        }
    }
}



