# Duke- added functionality: MongoDB as Datasource

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

## Parameters

- Required:
 * database
 * collection

- Optional (and default values):
 * server-address: "localhost"
 * port-number: "27017:
 * db-auth: "false" (other possible values: "true" and "admin", case unsensitive)
 * user-name: required if db-auth is set to "true" or "admin"
 * password: same as user-name
 * cursor-notimeout: "false" (can be set to "true")
 * query: "{}" (query all documents in the collection)
 * projection
 
## Behavior

1. If a parameter is invalid (port-number, server-address, collection, etc.) an error will be thrown.
2. When cursor-notimeout is not set (or set to "false") and BATCH_SIZE < db[collection].count(query), it's possible that when Duke is trying to fetch the rest of the data (it performs a MongoDB getmore) the cursor is already timed out. To prevent this, set cursor-notimeout to "true".
3. If db-auth is set to "true", the connection will try to find the credentials (user+password) in the DB specified in the database parameter. If is set to "admin", will try to find them in the admin DB. It this is set to a different value, this setting will be ignored and the field will preserve its default value.
4. Setting parameters to "" is the same that not setting them, except for port-number (an error with a custom message will be thrown).
5. The query and projection parameters have to be valid JSON formatted. There's no need to add quotes, even if they operate on a nested field. If format is not valid, an error will be thrown.
