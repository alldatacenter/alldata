# Apache Iceberg format plugin

This format plugin enabled Drill to query Apache Iceberg tables.

Unlike regular format plugins, the Iceberg table is a folder with data and metadata files, but Drill checks the presence
of the `metadata` folder to ensure that the table is Iceberg one.

Drill supports reading all formats of Iceberg tables available at this moment: Parquet, Avro, and ORC.
No need to provide actual table format, it will be discovered automatically.

For details related to Apache Iceberg table format, please refer to [official docs](https://iceberg.apache.org/#).

## Supported optimizations and features

### Project pushdown

This format plugin supports project and filter pushdown optimizations.

For the case of project pushdown, only columns specified in the query will be read, even they are nested columns. In
conjunction with column-oriented formats like Parquet or ORC, it allows improving reading performance significantly.

### Filter pushdown

For the case of filter pushdown, all expressions supported by Iceberg API will be pushed down, so only data that matches
the filter expression will be read.

### Schema provisioning

This format plugin supports the schema provisioning feature. Though Iceberg provides table schema, in some cases, it
might be useful to select data with customized schema, so it can be done using the table function:

```sql
SELECT int_field,
       string_field
FROM table(dfs.tmp.testAllTypes(schema => 'inline=(int_field varchar not null default `error`)'))
```

In this example, we convert int field to string and return `'error'` literals for null values.

### Querying table metadata

Apache Drill provides the ability to query any kind of table metadata Iceberg can return.

At this point, Apache Iceberg has the following metadata kinds:

* ENTRIES
* FILES
* HISTORY
* SNAPSHOTS
* MANIFESTS
* PARTITIONS
* ALL_DATA_FILES
* ALL_MANIFESTS
* ALL_ENTRIES

To query specific metadata, just add the `#metadata_name` suffix to the table location, like in the following example:

```sql
SELECT *
FROM dfs.tmp.`testAllTypes#snapshots`
```

### Querying specific table versions (snapshots)

Apache Iceberg has the ability to track the table modifications and read specific version before or after modifications
or modifications itself.

This format plugin embraces this ability and provides an easy-to-use way of triggering it.

The following ways of specifying table version are supported:

- `snapshotId` - id of the specific snapshot
- `snapshotAsOfTime` - the most recent snapshot as of the given time in milliseconds
- `fromSnapshotId` - read appended data from `fromSnapshotId` exclusive to the current snapshot inclusive
- \[`fromSnapshotId` : `toSnapshotId`\] - read appended data from `fromSnapshotId` exclusive to `toSnapshotId` inclusive

Table function can be used to specify one of the above configs in the following way:

```sql
SELECT *
FROM table(dfs.tmp.testAllTypes(type => 'iceberg', snapshotId => 123456789));

SELECT *
FROM table(dfs.tmp.testAllTypes(type => 'iceberg', snapshotAsOfTime => 1636231332000));

SELECT *
FROM table(dfs.tmp.testAllTypes(type => 'iceberg', fromSnapshotId => 123456789));

SELECT *
FROM table(dfs.tmp.testAllTypes(type => 'iceberg', fromSnapshotId => 123456789, toSnapshotId => 987654321));
```

## Configuration

Format plugin has the following configuration options:

- `type` - format plugin type, should be `'iceberg'`
- `properties` - Iceberg-specific table properties. Please refer to [Configuration](https://iceberg.apache.org/#configuration/) page for more details
- `caseSensitive` - whether table columns are case-sensitive

### Format config example:

```json
{
  "type": "file",
  "formats": {
    "iceberg": {
      "type": "iceberg",
      "properties": {
        "read.split.target-size": "134217728",
        "read.split.metadata-target-size": "33554432"
      },
      "caseSensitive": true
    }
  }
}
```
