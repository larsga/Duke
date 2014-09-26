## MongoDB as Datasource

This implementation based on no.priv.garshol.duke.datasources.JDBCDataSource.


## Example of use.

Suppose you have a collection named "newUsers" in a "gatheredData" DB. 

If this is the "schema":

```javascript
{
 _id: ObjectId("54107bb33f2a38e1e44e9961"),
 name:"Adolf",
 address:{
  street:"Rue Av.",
  number: 102,
  zip-code: 4106
 }
}
```

and you just want to consider "name" and "address.zip-code" fields:

```xml
<data-source class="no.priv.garshol.duke.datasources.MongoDBDataSource">
 <param name="server-address" value="domain.com"/>
 <param name="port-number" value="27017"/>
 <param name="database" value="gatheredData"/>
 <param name="collection" value="newUsers"/>
 <param name="projection" value="{_id:0, name:1, address.zip-code:1}"/>
 
 <column property="Name" name="name"/>
 <column property="ZipCode" name="address.zip-code"/>
</data-source>
```

Note that the fields "name" and "address.zip-code" have been mapped into "Name" and "ZipCode" in the column definition.

### Parameters

- Required:
 * database
 * collection

- Optional (and default values):
 * server-address: "localhost"
 * port-number: "27017:
 * db-auth: "false" (other possible values: "true" and "admin", case unsensitive)
 * user-name: required if db-auth is set to "true" or "admin"
 * password: required if db-auth is set to "true" or "admin"
 * cursor-notimeout: "false" (can be set to "true")
 * query: "{}" (query all documents in the collection)
 * projection: if not set, no projection will be performed
 
### Behavior

1. If a parameter is invalid (port-number, server-address, collection, etc.) an error will be thrown. The value for port-number must be a valid port number (between 1 and 65535), otherwhise the default value (27017) will be left.
2a. Setting a certain parameter to "" is the same as not setting it (commenting/omitting the assignment).
2b. If port-number it not going to be specified, it is preferable to comment (or omit) the assignment. An error with a specific message will be thrown if it's set to "" or to an unparseable string.
3. When cursor-notimeout is not set (or set to "false") and BATCH_SIZE < db[collection].count(query), it's possible that when Duke is trying to fetch the next batch (by performing a MongoDB getmore) the cursor is already timed out. To prevent this, set cursor-notimeout to "true".
4a. If db-auth is set to "true", the connection will try to find the credentials (user+password) in the DB specified by the database parameter. When set to "admin", it will try to find those credentials in the "admin" DB. 
4b. If db-auth is set to a value different from "false", "true" or "admin" (case insensitive), this setting will be ignored and the field will preserve its default value.
5. The query and projection parameters have to be valid JSON formatted. There's no need to add quotes, even if they operate on a nested field. If format is not valid, an error will be thrown.
6. You could skip the projection operator and perform the projection in the column definition, but a lot of very large documents would imply a significant slowdown caused by network traffic.
7. The query document can contain operators like $orderby, $showDiskLoc, etc. See http://docs.mongodb.org/manual/reference/operator/query-modifier/

### Future improvements
1. Deal with arrays of values and arrays of documents. Given that Duke supports multivalued columns, this is desired.
2. Accept different authentication methods.
3. Temporary index creation if needed.
4. Support for aggregate() command.