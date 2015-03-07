package no.priv.garshol.duke.datasources;

import com.mongodb.*;
import com.mongodb.util.JSON;
import org.bson.types.ObjectId;
import java.net.UnknownHostException;
import java.util.Arrays;

import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordIterator;
import no.priv.garshol.duke.DukeException;

// Implementation based on JDBCDataSource
public class MongoDBDataSource extends ColumnarDataSource {
  // connection params
  private static int MIN_PORT = 1;
  private static int MAX_PORT = 65535;
  private String mongouri = "localhost";	// default server
  private int port = 27017;					// default port
  
  // authentication params
  private static String AUTH_ON_ADMIN = "admin";
  private static String AUTH_ON_DB = "true";
  private static String AUTH_FALSE = "false";
  private String auth = AUTH_FALSE; 	// default value
  private String username;
  private String password;
  private boolean noTimeOut = false;	// by default we don't set that flag
  
  // query params
  private String dbname;
  private String collectionName;
  private String query = "{}";	// default: all documents
  private String projection;	// optional

  public MongoDBDataSource() {
    super();
  }

  // ----------
  // Setters: bean properties (Note: are "-" separated, instead of cammelCase formatted)
  // ----------
  
  public void setServerAddress(String addr) {
   if(!addr.equals("")){
     this.mongouri = addr;
    }
  }
  
  public void setPortNumber(String port) {
    int parsedPort;
    try{
      parsedPort = Integer.parseInt(port,10);
	  if(parsedPort>=MIN_PORT && parsedPort<=MAX_PORT){
        this.port = parsedPort;
      }
    }
	catch(NumberFormatException ex){
      System.out.println("** Invalid port number: "+port);
      throw new DukeException(ex);
	}
  }
  
  public void setDbAuth(String authdb){
    if(authdb.toLowerCase().equals(AUTH_ON_DB)){
      this.auth = AUTH_ON_DB;
    }
    /* I hate your 2 space identation, Lars... makes the code a little less unreadable*/
    else if(authdb.toLowerCase().equals(AUTH_ON_ADMIN)){
      this.auth = AUTH_ON_ADMIN;
    }
  }

  public void setUserName(String username) {
    if(!username.equals("")){
      this.username = username;
    }
  }

  public void setPassword(String password) {
    if(!password.equals("")){
      this.password = password;
    }
  }
  
  public void setDatabase(String dbname) {
    if(!dbname.equals("")){
      this.dbname = dbname;
    }
  }
  
  public void setCursorNotimeout(String timeout){
    if(timeout.toLowerCase().equals("true")){
		this.noTimeOut = true;
    }
  }
  
  public void setCollection(String collectionName) {
    if(!collectionName.equals("")){
      this.collectionName = collectionName;
    }
  }

  public void setQuery(String query) {
    if(!query.equals("")){
      this.query = query;
    }
  }
  
  public void setProjection(String projection) {
    if(!projection.equals("")){
      this.projection = projection;
    }
  }
  
  // ----------
  // Getters: we have to provide default values
  // ----------
  
  public String getServerAddress() {
    return mongouri;
  }
  
  public String getPortNumber() {
    return Integer.toString(port);
  }
  
  public String getDbAuth(){
	  return auth;
  }

  public String getUserName() {
    if(this.username==null){
    	return "";
    }
    return this.username;
  }

  public String getPassword() {
    if(this.password==null){
      return "";
    }
    return this.password;
  }
  
  public String getDatabase() {
    return this.dbname;
  }
  
  public String getCursorNotimeout(){
    if(this.noTimeOut){
      return "true";
    }
	else{
      return "false";
	}
  }
  
  public String getCollection() {
    return this.collectionName;
  }
  
  public String getQuery() {
    return this.query;
  }
  
  public String getProjection() {
    if(this.projection==null){
      return "";
    }
    return this.projection;
  }
  
  // ----------
  // Methods
  // ----------
  
  public RecordIterator getRecords() {
    verifyProperty(dbname, "database");
    verifyProperty(collectionName, "collection");
    
    try {
      final MongoClient mongo;
      final DB database;
      final DBCollection collection;
      final DBCursor result;
      
      final DBObject queryDocument;
      final DBObject projectionDocument;
      
	  // authentication mecanism via MONGODB-CR authentication http://docs.mongodb.org/manual/core/authentication/#authentication-mongodb-cr
      if(auth.equals(AUTH_ON_DB)){
        verifyProperty(username, "user-name");
        verifyProperty(password, "password");
        
        mongo = new MongoClient(
          new ServerAddress(mongouri, port), 
          Arrays.asList(MongoCredential.createMongoCRCredential(username, dbname, password.toCharArray()))
        );
      }
      else if(auth.equals(AUTH_ON_ADMIN)){
        verifyProperty(username, "user-name");
        verifyProperty(password, "password");
        
  		mongo = new MongoClient(
          new ServerAddress(mongouri, port), 
          Arrays.asList(MongoCredential.createMongoCRCredential(username, AUTH_ON_ADMIN, password.toCharArray()))
        );
      }
      else{
        mongo = new MongoClient(new ServerAddress(mongouri, port));
      }
      
      // get db, collection
      database = mongo.getDB(dbname);
      collection = database.getCollection(collectionName);
      
      // execute query
      queryDocument = (DBObject)JSON.parse(query);
      if(projection==null){
        result = collection.find(queryDocument);
      }
      else{
          projectionDocument = (DBObject)JSON.parse(projection);
          result = collection.find(queryDocument, projectionDocument);
      }
      
      // See: http://api.mongodb.org/java/current/com/mongodb/DBCursor.html#addOption(int)
      // and http://api.mongodb.org/java/current/com/mongodb/Bytes.html#QUERYOPTION_NOTIMEOUT
      if(noTimeOut){
        result.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
      }
	  
      return new MongoDBIterator(result, mongo);
    
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    } catch (Exception ex){
	  throw new DukeException(ex);
    }
  }

  protected String getSourceName() {
    return "MongoDB";
  }


  // Nested class that will return the flattened MongoDB documents
  public class MongoDBIterator extends RecordIterator {
    private DBCursor cursor;
    private MongoClient mongoClient;
    private DBObject element;
    private boolean hasNext;
    private RecordBuilder builder;
    private static final String DOT = ".";

    public MongoDBIterator(DBCursor cursor, MongoClient mongoClient) throws MongoException{
      this.mongoClient = mongoClient;
      this.cursor = cursor;
      this.hasNext = cursor.hasNext();
      this.builder = new RecordBuilder(MongoDBDataSource.this);
    }
    
    @Override
    public boolean hasNext() {
      return hasNext;
    }
    
    @Override
    public Record next() {
      try {
        element = cursor.next();
        builder.newRecord();
    	
        for (Column col : getColumns()) {
          // TODO: identify arrays (containing values or DBObjects) in order to add multiple values
          String value = getStringValueFromCursorElement(element, col.getName());
          builder.addValue(col, value);
        }

        hasNext = cursor.hasNext(); // step to next

        return builder.getRecord();
      } catch (MongoException e) {
        throw new RuntimeException(e);
      }
    }

    // Recursive function which iterates through [sub-sub-sub...]documents
    // 	NOTE: this assummes that DOT means field nesting
    // 		When the DOT is actually part of the field name, it does not work properly
    // 		If this is your case, one day you will have to update it: http://docs.mongodb.org/manual/release-notes/2.6-compatibility/#updates-enforce-field-name-restrictions
    private String getStringValueFromCursorElement(DBObject elem, String propName){
      int dotIndex = propName.indexOf(DOT);
      Object value;
      DBObject subValue;
      String subPropName;
      String propNameSuffix;

      if(dotIndex==-1){
        value = elem.get(propName);
        
        if(value instanceof String){
          return (String)value;
        }
        else if(value==null){
          return null;
        }
        else{
          return value.toString();
        }
      }
      else{
  	    propNameSuffix = propName.substring(0,dotIndex);
        subValue = (DBObject)elem.get(propNameSuffix);
        subPropName = propName.substring(dotIndex+1);
        return getStringValueFromCursorElement(subValue, subPropName);
      }
    }
    
    @Override	// is this "Override" necessary?
    public void close(){
      try {
        mongoClient.close();
      } catch (Exception e) {
        throw new DukeException(e);
      }
    }
    
  }
}
