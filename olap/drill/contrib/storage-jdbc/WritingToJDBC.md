# Writing to JDBC Data Sources
It is now possible to write to databases via Drill's JDBC Storage Plugin.  At present Drill supports the following query formats for writing:

* `CREATE TABLE AS`
* `CREATE TABLE IF NOT EXISTS`
* `DROP TABLE`
* `DROP TABLE IF NOT EXISTS`

For further information about Drill's support for CTAS queries please refer to the documentation page here: https://drill.apache.org/docs/create-table-as-ctas/. The syntax is 
exactly the same as writing to a file.  As with writing to files, it is a best practice to avoid `SELECT *` queries in the CTAS query. 

Not all JDBC sources will support writing. In order for the connector to successfully write, the source system must support `CREATE TABLE AS` as well as `INSERT` queries.  
At present, Writing has been tested with MySQL, Postgres and H2.

#### Note about Apache Phoenix
Apache Phoenix uses slightly non-standard syntax for INSERTs.  The JDBC writer should support writes to Apache Phoenix though this has not been tested and should be regarded as 
an experimental feature.

## Configuring the Connection for Writing
Firstly, it should go without saying that the Database to which you are writing should have a user permissions which allow writing.  Next, you will need to set the `writable` 
parameter to `true` as shown below:

### Setting the Batch Size
Drill after creating the table, Drill will execute a series of `INSERT` queries with the data you are adding to the new table.  How many records can be inserted into the 
database at once is a function of your specific database.  Larger numbers will result in fewer insert queries, and more likely faster overall performance, but may also overload 
your database connection.  You can configure the batch size by setting the `writerBatchSize` variable in the configuration as shown below.  The default is 10000 records per batch.

### Sample Writable MySQL Connection
```json
{
  "type": "jdbc",
  "driver": "com.mysql.cj.jdbc.Driver",
  "url": "jdbc:mysql://localhost:3306/?useJDBCCompliantTimezoneShift=true&serverTimezone=EST5EDT",
  "username": "<username>",
  "password": "<password>",
  "writable": true,
  "writerBatchSize": 10000,
  "enabled": true
}
```
### Sample Writable Postgres Connection
```json
{
  "type": "jdbc",
  "driver": "org.postgresql.Driver",
  "url": "jdbc:postgresql://localhost:5432/sakila?defaultRowFetchSize=2",
  "username": "postgres",
  "sourceParameters": {
    "minimumIdle": 5,
    "autoCommit": false,
    "connectionTestQuery": "select version() as postgresql_version",
    "dataSource.cachePrepStmts": true,
    "dataSource.prepStmtCacheSize": 250
  },
  "writable": true
}
```

## Limitations

### Row Limits
The first issue to be aware of is that most relational databases have some sort of limit on how many rows can be inserted at once and how many columns a table may contain.  It 
is important to be aware of these limits and make sure that your database is configured to receive the amount of data you are trying to write.  For example, you can configure 
MySQL by setting the `max_packet_size` variable to accept very large inserts.

### Data Types
While JDBC is a standard for interface, different databases handle datatypes in different manners.  The JDBC writer tries to map data types to the most generic way possible so 
that it will work in as many cases as possible. 

#### Compound Data Types
Most relational databases do not support compound fields of any sort.  As a result, attempting to write a compound type to a JDBC data source, will result in an exception. 
Future functionality may include the possibility of converting complex types to strings and inserting those strings into the target database.

#### VarBinary Data
It is not currently possible to insert a VarBinary field into a JDBC database.
