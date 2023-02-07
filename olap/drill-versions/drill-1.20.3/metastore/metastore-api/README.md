# Metastore

Metastore can store metadata information for tables, views, etc.
Such metadata can be used during queries execution for better planning,
obtaining additional information, etc.

### Configuration

All configuration properties should reside in `drill.metastore` namespace.

Default Metastore configuration is defined in `drill-metastore-default.conf` file.
It can be overridden in `drill-metastore-override.conf`. Distribution configuration can be
indicated in `drill-metastore-distrib.conf`. Metastore implementations configuration can be
indicated in `drill-metastore-module.conf`.

## Initialization

`MetastoreRegistry` is initialized during Drillbit start up and is accessible though `DrillbitContext`.
It lazily initializes Metastore implementation based
on class implementation config property `drill.metastore.implementation.class`.

Metastore implementation must implement `Metastore` interface and
have constructor which accepts `DrillConfig`.

### Metastore Components

Metastore can store metadata for various components: tables, views etc.
Current implementation provides fully functioning support for tables component.
Views component support is not implemented but contains stub methods to show
how new Metastore components like udfs, storage plugins, etc. be added in future.

### Metastore Tables

Metastore Tables component contains metadata about Drill tables, including general information, as well as
information about table segments, files, row groups, partitions.

`TableMetadataUnit` is a generic representation of Metastore Tables metadata unit, 
suitable to any table metadata type (table, segment, file, row group, partition).
`BaseTableMetadata`, `SegmentMetadata`, `FileMetadata`, `RowGroupMetadata` and `PartitionMetadata`
classes which are located in `org.apache.drill.metastore.metadata` package, have methods 
to convert to / from `TableMetadataUnit`.

Full table metadata consists of two major concepts: general information and top-level segments metadata.
Table general information contains basic table information and corresponds to `BaseTableMetadata` class.

Table can be non-partitioned and partitioned. Non-partitioned tables, have only one top-level segment 
which is called default (`MetadataInfo#DEFAULT_SEGMENT_KEY`). Partitioned tables may have several top-level segments.
Each top-level segment can include metadata about inner segments, files, row groups and partitions.

Unique table identifier in Metastore Tables is combination of storage plugin, workspace and table name.
Table metadata inside is grouped by top-level segments, unique identifier of the top-level segment and its metadata
is storage plugin, workspace, table name and metadata key.

### Metadata

In order to provide Metastore component metadata functionality `Metadata` interface must be implemented.
`Metadata` interface implementation can be specific for each Metastore component or shared.
For example, for Iceberg Metastore, each component will provide its own `Metadata` interface implementation
since each component is stored in separate Iceberg tables. For other Metastore implementations, if components reside
in the same storage (database), `Metadata` interface implementation can be shared.

#### Versioning

Metastore component may or may not support versioning depending on the implementation.
`Metadata#supportsVersioning` and `Metastore.Metadata#version` methods 
are used to indicate versioning support. If Metastore component does not support versioning, 
`Metadata#version` returns undefined version (`Metadata#UNDEFINED`).
If Metastore component supports versioning, it is assumed that version is changed each time
data in the Metastore component is modified and remains the same during read operations.
Metastore component version is used to determine if metadata has changed after last access of the Metastore component.

#### Properties

Metastore component may or may not support properties depending on the implementation.
If properties are supported, map with properties names and values is returned.
otherwise empty map is returned. `Metadata#properties` is used to obtain properties information. 

### Data filtering

Metastore data can be read or deleted based on the filter expression and metadata types.

#### Metadata types

Each concrete Metastore component implementation supports specific metadata types
which identify metadata inside Metastore component units. For example, for `tables`
component, supported metadata types are` TABLE`, `SEGMENT`, `FILE`, `ROW_GROUP`, `PARTITION`.
Metadata types are based on `org.apache.drill.metastore.metadata.MetadataType` enum names.

Metadata types should be indicated during read or delete operations.
If all metadata types are needed, `MetadataType#ALL` metadata type can be indicated.

#### Filter expressions

All filter expressions implement `FilterExpression` interface. 
List of supported filter operators is indicated in `FilterExpression.Operator` enum.
When filter expression is provided in read or delete operation, it's up to Metastore
implementation to convert it into suitable representation for storage.
For convenience, `FilterExpression.Visitor` can be implemented to traverse filter expression.

Filter expression can be simple and contain only one condition:

```
FilterExpression storagePlugin = FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs");
FilterExpression workspaces = FilterExpression.in(MetastoreColumn.WORKSPACE, "root", "tmp");

```

Or it can be complex and contain several conditions combined with `AND` or `OR` operators.

```
  FilterExpression filter = FilterExpression.and(
    FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs"),
    FilterExpression.in(MetastoreColumn.WORKSPACE, "root", "tmp"));
  
  metastore.tables().read()
    .metadataType(MetadataType.TABLE)
    .filters(filter)
    .execute();
```

SQL-like equivalent for the above operation is:

```
  select * from METASTORE.TABLES
  where storagePlugin = 'dfs'
  and workspace in ('root', 'tmp')
  and metadataType = 'TABLE'
```

### Metastore Read

In order to provide read functionality each component must implement `Read`.
During implementation component unit type must be indicated.
`Metastore.Read#columns` allows to specify list of columns to be retrieved from the Metastore component.
Columns are represented by `MetastoreColumn` enum values.
`Metastore.Read#filter` allows to specify filter expression by which data will be retrieved.
`Metastore.Read#execute` executes read operation and returns the results.
Data is returned in a form of list of component metadata units, it is caller responsibility to transform received
data into suitable representation. It is expected, if no result is found, empty of list of units
will be returned, not null instance.

For `tables` component, metadata unit is represented by `TableMetadataUnit`. To retrieve `lastModifiedTime` 
for all tables in the `dfs` storage plugin the following code can be used:

```
  List<TableMetadataUnit> units = metastore.tables().read()
    .metadataType(MetadataType.TABLE)
    .columns(MetastoreColumn.TABLE_NAME, MetastoreColumn.LAST_MODIFIED_TIME)
    .filter(FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs")
    .execute();
```

### Metastore Tables Basic Requests

`Tables#basicRequests` provides list of most frequent requests to the Metastore Tables without need 
to write filters and transformers from `TableMetadataUnit` class.

Assume caller needs to obtain general metadata about `dfs.tmp.nation` table: 

```
    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name("nation")
      .build();

    BaseTableMetadata metastoreTableInfo = metastore.tables().basicRequests()
      .tableMetadata(tableInfo);
```

### Metastore Modify

In order to provide read functionality each component must implement `Modify`.
During implementation component unit type must be indicated.

If Metastore component implementation supports versioning, it is assumed that each modify operation will
change Metastore component version.

#### Overwrite

`Read#overwrite` writes data into Metastore component or overwrites existing data by unique partition 
keys combination. 

For example, for `tables` component, partition keys are storage plugin, workspace, table name 
and metadata key. Caller provides only list of `TableMetadataUnit` to be written 
and Metastore component implementation will decide how data will be stored or overwritten.

##### Adding new table metadata

Assume there is non-partitioned table which metadata is represented with two units.

1. Unit with table general information:

```
    TableMetadataUnit tableUnit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE.name())
      ...
      .build();
```

2. Unit with default segment information with one file:

```
    TableMetadataUnit segmentUnit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .metadataType(MetadataType.SEGMENT.name())
      ...
      .build();
      
    TableMetadataUnit fileUnit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .metadataType(MetadataType.FILE.name())
      ...
      .build();
```

To add this table metadata in the Metastore, the following code can be executed:

```
    metastore.tables().modify()
      .overwrite(tableUnit, segmentUnit)
      .execute();
```

##### Overwriting table metadata

Metastore allows only to overwrite metadata by unique combination of table identifier 
or table metadata identifiers (general info or top-level segments).

When only general table information has changed but segments default metadata did not change,
it is enough to overwrite only general information.

```
    TableMetadataUnit updatedTableUnit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE.name())
      ...
      .build();
      
    metastore.tables().modify()
      .overwrite(updatedTableUnit)
      .execute();
```

If segment metadata was changed, new file was added to the default segment, 
all segment information must be overwritten.

```   
    TableMetadataUnit segmentUnit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .metadataType(MetadataType.SEGMENT.name())
      ...
      .build();
      
    TableMetadataUnit initialFileUnit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .metadataType(MetadataType.FILE.name())
      ...
      .build();
      
    TableMetadataUnit newFileUnit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .metadataType(MetadataType.FILE.name())
      ...
      .build();  
      
    metastore.tables().modify()
      .overwrite(segmentUnit, initialFileUnit, newFileUnit)
      .execute();
```

#### Delete

`Read#delete` deletes data from the Metastore component based on the provided delete operation.

Delete operation consists of delete filter and metadata types.

Assume metadata for table `dfs.tmp.nation` already exists in the Metastore `tables` component
and caller needs to delete it and all its metadata. First, deletion filter must be created:

```
    FilterExpression deleteFilter = FilterExpression.and(
      FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs"),
      FilterExpression.equal(MetastoreColumn.WORKSPACE, "tmp"),
      FilterExpression.equal(MetastoreColumn.TABLE_NAME, "nation"));
```

Such filter can be also generated using `TableInfo` class:

```
    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name("nation")
      .build();
      
    FilterExpression deleteFilter = tableInfo.toFilter();  
```

Then metadata type should be indicated, since all table metadata needs to be deleted,
`ALL` metadata type should be specified.

```
    Delete deleteOperation = Delete.builder()
      .metadataType(MetadataType.ALL)
      .filter(deleteFilter)
      .build()
```

Delete operation can be executed using the following code:

```
    metastore.tables().modify()
      .delete(deleteOperation)
      .execute();
```

#### Purge

`Read#purge` deletes all data from the Metastore component. Purge is terminal operation
and cannot be used together with overwrite or delete.

```
    metastore.tables().modify()
      .purge();
```

#### Transactions

Metastore component implementation may or may not support transactions. If transactions are supported,
all operations in one `Modify` instance will be executed fully or not executed at all.
If Metastore implementation does not support transactions, all operations will be executed consequently.
Note: operations will be executed in the same order as they were added.

```
    metastore.tables().modify()
      .overwrite(tableUnit1, segmentUnit1)
      .overwrite(tableUnit2, segmentUnit2)
      .delete(deleteOperation1)
      .delete(deleteOperation2)
      .execute();
```
