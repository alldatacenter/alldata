# Iceberg Metastore

Iceberg Metastore implementation is based on [Iceberg tables](http://iceberg.incubator.apache.org). 
One Iceberg table corresponds to one metastore component (`tables`, `views`, etc,)
Iceberg tables schemas (field names and types) are based on component metadata units classes fields 
annotated with `MetastoreFieldDefinition` annotation.

## Configuration

Iceberg Metastore configuration is indicated in `drill-metastore-module.conf` and 
can be overwritten in `drill-metastore-distrib.conf` or  `drill-metastore-override.conf` files.

`drill.metastore.config.properties` - allows to specify properties connected with file system.

`drill.metastore.iceberg.location.base_path` and `drill.metastore.iceberg.location.relative_path` -
indicate Iceberg Metastore base location.

`drill.metastore.iceberg.expiration.period` - allows to specify period after which Iceberg table outdated
metadata will be expired. Unit names must correspond to `java.time.temporal.ChronoUnit` enum values
that do not have estimated duration (millis, seconds, minutes, hours, days).

`drill.metastore.iceberg.components` - provides configuration for specific Metastore components:
`drill.metastore.iceberg.components.tables`, `drill.metastore.iceberg.components.views`.

`drill.metastore.iceberg.common.properties` - allows to specify common for all Iceberg tables,
refer to `org.apache.iceberg.TableProperties` class for the list of available table properties. 
Each component can specify specific table properties which override the same common table properties:
`drill.metastore.iceberg.components.tables.properties`, `drill.metastore.iceberg.components.views.properties`.

Each component must specify Iceberg table location within Iceberg Metastore base location:
`drill.metastore.iceberg.components.tables.location`, `drill.metastore.iceberg.components.views.location`.

## Iceberg Tables Location

Iceberg tables will reside on the file system in the location based on
Iceberg Metastore base location and component specific location.
If Iceberg Metastore base location is `/drill/metastore/iceberg`
and tables component location is `tables`. Iceberg table for tables component
will be located in `/drill/metastore/iceberg/tables` folder.

Inside given location, Iceberg table will create `metadata` folder where 
it will store its snapshots, manifests and table metadata.

Note: Iceberg table supports concurrent writes and transactions 
but they are only effective on file systems that support atomic rename.

## Component Data Location

Metastore metadata will be stored inside Iceberg table location provided
in the configuration file. Drill table metadata location will be constructed
based on specific component storage keys. For example, for `tables` component,
storage keys are storage plugin, workspace and table name: unique table identifier in Drill.

Assume Iceberg table location is `/drill/metastore/iceberg/tables`, metadata for the table
`dfs.tmp.nation` will be stored in the `/drill/metastore/iceberg/tables/dfs/tmp/nation` folder.

### Metadata Storage Format

Iceberg tables support data storage in three formats: Parquet, Avro, ORC.
Drill metadata will be stored in Parquet files. This format was chosen over others
since it is column oriented and efficient in terms of disk I/O 
when specific columns need to be queried.

Each Parquet file will hold information for one partition.
Partition keys will depend on Metastore component characteristics.
For example, for `tables` component, partitions keys are
storage plugin, workspace, table name and metadata key.

Parquet files name will be based on `UUID` to ensure uniqueness.
If somehow collision occurs, modify operation in Metastore will fail.

## Metastore Operations flow

Metastore main goal is to provide ability to read and modify metadata.

### Read

Metastore data is read using `IcebergGenerics#read`.
Based on given filter and select columns list, data will be returned in 
`org.apache.iceberg.data.Record` format which will be transformed 
into the list of Metastore component units and returned to the caller.
For example, for `tables` component, metadata unit is `TableMetadataUnit`.

Iceberg supports partition pruning, to avoid scanning all data and improve performance,
partition keys can be included into filter expression.

### Add

To add metadata to Iceberg table, caller provides list of component units which
will be written into Parquet files and grouped by partition keys.
Each group will be written into separate Parquet file 
and stored in the location inside of Iceberg table based on component unit location keys.
Note: partition keys must not be null.

For example, for `tables` component data is grouped by storage plugin, workspace, table name
and metadata key, location is determined based on storage plugin, workspace and table name.

For each Parquet file, `IcebergOperation#Overwrite` will be created.
List of overwrite operations will be executed in one transaction.
Main goal of overwrite operation to add or replace pointer of the existing file
in Iceberg table's partition information.

If transaction was successful, Iceberg table generates new snapshot and updates
its own metadata.

Assume, caller wants to add metadata for Drill table `dfs.tmp.nation` into `tables` component.
Parquet files with metadata for this table will be stored in 
`[METASTORE_ROOT_DIRECTORY]/[COMPONENT_LOCATION]/dfs/tmp/nation` folder.

If `dfs.tmp.nation` is un-partitioned, it's metadata will be stored in two
Parquet files: one file with general table information, 
another file with default segment information. 
If `dfs.tmp.nation` is partitioned, it will have also one file with general
information and `N` files with top-level segments information. 

File with general table information will always have one row.
Number of rows for default or top-level segment file will depend on segments 
metadata. Each row corresponds to one metadata unit: segment, file,
row group or partition.

If `dfs.tmp.nation` table has one segment to which belongs one file 
with two row groups and one partition and no inner segments, 
this segment's file will have five rows: 
- one row with top-level segment metadata;
- one row with file metadata;
- two rows with row group metadata;
- one row with partition metadata.

Such storage model allows easily to overwrite or delete existing top-level segments
without necessity to re-write all table metadata when it was only partially changed.

When `dfs.tmp.nation` metadata is created, table metadata location will store two files.
File names are generated based on `UUID` to ensure uniqueness.

```
...\dfs\tmp\nation\282205e0-88f2-4df2-aa3c-90d26ae0ad28.parquet (general info file)
...\dfs\tmp\nation\e6b20998-d640-4e53-9ece-5a063e498e1a.parquet (default segment file)

```

Once overwrite operation is committed, Iceberg table will have two partitions:
`\dfs\tmp\nation\GENERAL_INFO` and `\dfs\tmp\nation\DEFAULT_SEGMENT`.
Each of these partitions will point to dedicated Parquet file.
If table is partitioned, instead of `DEFAULT_SEGMENT`, top-level
segment metadata key will be indicated. For example, `\dfs\tmp\nation\dir0`.

### Overwrite

Process of overwriting existing partitions is almost the same as adding new partition.
Assume, `dfs.tmp.nation` default segment metadata has changed: new file was added.
Caller passes updated default segment metadata to the Metastore.
First, new Parquet file for updated default segment metadata is created.
Once overwrite operation is committed, `\dfs\tmp\nation\DEFAULT_SEGMENT` partition
will point to Parquet file with updated metadata. 
Table metadata location will store three files:

```
...\dfs\tmp\nation\282205e0-88f2-4df2-aa3c-90d26ae0ad28.parquet (general info file)
...\dfs\tmp\nation\e6b20998-d640-4e53-9ece-5a063e498e1a.parquet (old segment file)
...\dfs\tmp\nation\4930061e-1c1d-4c8e-a19e-b7b9a5f5f246.parquet (new segment file)

```

### Delete

To delete data from Iceberg table, caller provides filter by which data will be deleted.
Filter expression must be based on component partition keys.

Delete operation removes partitions from Iceberg table, it does not remove data files to which
these partitions were pointing. Outdated data files will be deleted during expiration process.

If delete operation was successful, Iceberg table generates new snapshot and updates
its own metadata.

### Purge

Allows to delete all data from Iceberg table. During this operation Iceberg table
is not deleted, history of all operations and data files are preserved until
expiration process is launched.

## Iceberg metadata expiration

Iceberg table generates metadata for each modification operation:
snapshot, manifest file, table metadata file. Also when performing delete operation,
previously stored data files are not deleted. These files with the time
can occupy lots of space. Two table properties `write.metadata.delete-after-commit.enabled`
and `write.metadata.previous-versions-max` control expiration process.
Metadata files will be expired automatically if `write.metadata.delete-after-commit.enabled` 
is enabled. Snapshots and data files will be expired using `ExpirationHandler` 
after each commit operation based on the same table properties values.
