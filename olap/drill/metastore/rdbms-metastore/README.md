# RDBMS Metastore

The RDBMS Metastore implementation allows you store Drill Metastore metadata in a configured RDBMS.

## Configuration

Currently, the RDBMS Metastore is not the default implementation.
To enable the RDBMS Metastore create the `drill-metastore-override.conf` file 
in your config directory and specify the RDBMS Metastore class:

```
drill.metastore: {
  implementation.class: "org.apache.drill.metastore.rdbms.RdbmsMetastore"
}
```

### Connection properties

Use the connection properties to specify how Drill should connect to your Metastore database.

`drill.metastore.rdbms.data_source.driver` - driver class name. Required. 
Note: the driver class must be included into the Drill classpath. 
The easiest way to do that is to put the driver jar file into the `$DRILL_HOME/jars/3rdparty` folder.
Or, to make upgrades easier, in your `$DRILL_SITE/jars` folder.
Drill includes the driver for SQLite.

`drill.metastore.rdbms.data_source.url` - connection url. Required.

`drill.metastore.rdbms.data_source.username` - database user on whose behalf the connection is
being made. Optional, if database does not require user to connect. 

`drill.metastore.rdbms.data_source.password` - database user's password. 
Optional, if database does not require user's password to connect.

`drill.metastore.rdbms.data_source.properties` - specifies properties which will be used
during data source creation. See list of available [Hikari properties](https://github.com/brettwooldridge/HikariCP)
for more details.

### Default configuration 

Out of the box, the Drill RDBMS Metastore is configured to use the embedded file system based SQLite database.
It will be created locally in user's home directory under `${drill.exec.zk.root}"/metastore` location.

Default setup can be used only in Drill embedded mode. SQLite is an embedded database; is not distributed. 
SQLite is good for trying out the feature, for testing, for a running Drill in embedded mode, 
and perhaps for a single-node Drill "cluster". If should not be used in a multi-node cluster. 
Each Drillbit will have its own version of the truth and behavior will be undefined and incorrect.

### Custom configuration

`drill-metastore-override.conf` is used to customize connection details to the Drill Metastore database.
See `drill-metastore-override-example.conf` for more details.

#### Example of PostgreSQL configuration

```
drill.metastore: {
  implementation.class: "org.apache.drill.metastore.rdbms.RdbmsMetastore",
  rdbms: {
    data_source: {
      driver: "org.postgresql.Driver",
      url: "jdbc:postgresql://localhost:1234/mydb?currentSchema=drill_metastore",
      username: "user",
      password: "password"
    }
  }
}
```

Note: as mentioned above, the PostgreSQL JDBC driver must be present in the Drill classpath.

#### Example of MySQL configuration

```
drill.metastore: {
  implementation.class: "org.apache.drill.metastore.rdbms.RdbmsMetastore",
  rdbms: {
    data_source: {
      driver: "com.mysql.cj.jdbc.Driver",
      url: "jdbc:mysql://localhost:1234/drill_metastore",
      username: "user",
      password: "password"
    }
  }
}
```

Note: as mentioned above, the MySQL JDBC driver must be present in the Drill classpath.

##### Driver version

For MySQL connector version 6+, use the `com.mysql.cj.jdbc.Driver` driver class,
for older versions use the `com.mysql.jdbc.Driver`.

## Tables structure

The Drill Metastore stores several types of metadata, called components. Currently, only the `tables` component is implemented.
The `tables` component provides metadata about Drill tables, including their segments, files, row groups and partitions.
In Drill `tables` component unit is represented by `TableMetadataUnit` class which is applicable to any metadata type.
The `TableMetadataUnit` class holds fields for all five metadata types within the `tables` component. 
Any fields not applicable to a particular metadata type are simply ignored and remain unset.

In the RDBMS implementation of the Drill Metastore, the tables component includes five tables, one for each metadata type. 
The five tables are: `TABLES`, `SEGMENTS`, `FILES`, `ROW_GROUPS`, and `PARTITIONS`.
See `src/main/resources/db/changelog/changes/initial_ddls.yaml` for the schema and indexes of each table.

The Drill Metastore API has the following semantics:
* most of the time all data about component is accessed;
* data is filtered by non-complex fields, like storage plugin, workspace, table name, etc;
* data is overwritten fully, there is no update by certain fields.

Taking into account the Drill Metastore API semantics, the RDBMS Drill Metastore schema is slightly denormalized.
Having normalized structure would lead to unnecessary joins during select, index re-indexing during update.

### Table creation

The RDBMS Metastore uses [Liquibase](https://www.liquibase.org/documentation/core-concepts/index.html)
to create the needed tables during the RDBMS Metastore initialization. Users should not create any tables manually.

### Database schema

Liquibase uses a yaml configuration file to apply changes to the database schema: `src/main/resources/db/changelog/changelog.yaml`.
Liquibase converts the yaml specification into the DDL / DML commands suitable required for the configured database.
See list of supported databases: https://www.liquibase.org/databases.html.

The Drill Metastore tables are created in the database schema indicated in the connection URL.
This will be the default schema unless you specify a different schema. Drill will not create the schema, however. 
Best practice is to create a schema within your database for the Drill metastore before initializing the Metastore.

Example:

PostgreSQL: `jdbc:postgresql://localhost:1234/mydb?currentSchema=drill_metastore`

MySQL: `jdbc:mysql://localhost:1234/drill_metastore`

Since Drill will create the required tables, ensure that the database user has the following permissions in the metastore schema:
* read and write tables;
* create and modify database objects (tables, indexes, views, etc.).

### Liquibase tables

During Drill RDBMS Metastore initialization, Liquibase will create two internal tracking tables:
`DATABASECHANGELOG` and `DATABASECHANGELOGLOCK`. They are needed to track schema changes and concurrent updates.
See https://www.liquibase.org/get_started/how-lb-works.html for more details.

## Query execution

SQL queries issued to RDBMS Metastore tables are generated using [JOOQ](https://www.jooq.org/doc/3.13/manual/getting-started/).
Drill uses the open-source version of JOOQ to generate the queries sent to the configured Metastore database.

JOOQ generates SQL statements based on SQL dialect determined by database connection details.
List of supported dialects: https://www.jooq.org/javadoc/3.13.x/org.jooq/org/jooq/SQLDialect.html.
Note: dialects annotated with `@Pro` are not supported, since open-source version of JOOQ is used.

## Supported databases

The RDBMS Metastore was tested with `SQLite`, `PostreSQL` and `MySQL`. Other databases should also work
if there is Liquibase and JOOQ support for them.
