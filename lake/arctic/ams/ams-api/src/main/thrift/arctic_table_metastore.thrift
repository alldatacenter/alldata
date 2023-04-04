namespace java com.netease.arctic.ams.api

include "arctic_commons.thrift"

struct CatalogMeta {
  1: required string catalogName;
  2: required string catalogType;
  3: map<string, string> storageConfigs;
  4: map<string, string> authConfigs;
  5: map<string, string> catalogProperties;
}


struct PartitionFieldData {
    1: string name;
    2: string value;
}

struct DataFile {
    // base_file, insert_file, delete_file
    1: string fileType;
    2: string path;
    // file size in bytes
    3: i64 fileSize;
    4: i64 mask;
    5: i64 index;
    6: i64 specId;
    7: list<PartitionFieldData> partition;
    8: i64 recordCount;
    // deprecated, may remove from v0.5
    9: map<string, binary> upperBounds;
}

struct TableChange {
    // base, change
    1: string innerTable;
    2: list<DataFile> addFiles;
    3: list<DataFile> deleteFiles;
    4: i64 snapshotId;
    5: i64 parentSnapshotId;
    6: i64 snapshotSequence;
}

// task commit info
struct TableCommitMeta {
    1: arctic_commons.TableIdentifier tableIdentifier;
    2: string action;
    3: list<TableChange> changes;
    4: i64 commitTime;
    5: map<string, string> properties;
    6: CommitMetaProducer commitMetaProducer;
    7: SchemaUpdateMeta schemaUpdateMeta;
}

struct UpdateColumn {
    1: string name;
    2: optional string parent;
    3: optional string type;
    4: optional string doc;
    5: string operate;
    6: optional string isOptional;
    7: optional string newName;
}

struct SchemaUpdateMeta {
    1: i32 schemaId;
    2: list<UpdateColumn> updateColumns;
}

struct TableMeta {
    1:arctic_commons.TableIdentifier tableIdentifier;
    2:PrimaryKeySpec keySpec;
    3:optional map<string, string> properties;
    4:map<string, string> locations ;
}

struct PrimaryKeySpec {
  1: list<string> fields;
}

enum CommitMetaProducer {
    OPTIMIZE,
    INGESTION
}

struct Blocker {
    1:string blockerId;
    2:list<BlockableOperation> operations;
    3:map<string, string> properties;
}

enum BlockableOperation {
   OPTIMIZE,
   BATCH_WRITE
}

/**
* replace TableContainer、ArcticTableItem
**/
service ArcticTableMetastore {

    void ping()

    // catalog api
    list<CatalogMeta> getCatalogs()

    CatalogMeta getCatalog(1: string name) throws (1: arctic_commons.NoSuchObjectException e1)

    list<string> getDatabases(1: string catalogName) throws (1: arctic_commons.NoSuchObjectException e)

    void createDatabase(1: string catalogName, 2: string database) throws (
          1: arctic_commons.NoSuchObjectException e1,
          2: arctic_commons.AlreadyExistsException e2)

    void dropDatabase(1: string catalogName, 2: string database) throws (
          1: arctic_commons.NoSuchObjectException e1,
          2: arctic_commons.NotSupportedException e2)

    void createTableMeta(1: TableMeta tableMeta)
          throws(
          1: arctic_commons.AlreadyExistsException e1,
          2: arctic_commons.InvalidObjectException e2
          3: arctic_commons.MetaException e3)

    list<TableMeta> listTables(1: string catalogName, 2: string database)
        throws(1: arctic_commons.NoSuchObjectException e);

    TableMeta getTable(1:arctic_commons.TableIdentifier tableIdentifier)
            throws(1: arctic_commons.NoSuchObjectException e);

    void removeTable(1:arctic_commons.TableIdentifier tableIdentifier, 2:bool deleteData)
        throws(
        1:arctic_commons.NoSuchObjectException e1,
        2:arctic_commons.MetaException e2)

    void tableCommit(1: TableCommitMeta commit) throws (1: arctic_commons.MetaException e1)

    i64 allocateTransactionId(1:arctic_commons.TableIdentifier tableIdentifier, 2:string transactionSignature)
    
    Blocker block(1:arctic_commons.TableIdentifier tableIdentifier, 2:list<BlockableOperation> operations, 3:map<string, string> properties) 
        throws (1: arctic_commons.OperationConflictException e1)
        
    void releaseBlocker(1:arctic_commons.TableIdentifier tableIdentifier, 2:string blockerId)
    
    i64 renewBlocker(1:arctic_commons.TableIdentifier tableIdentifier, 2:string blockerId)
        throws(1: arctic_commons.NoSuchObjectException e)
    
    list<Blocker> getBlockers(1:arctic_commons.TableIdentifier tableIdentifier)

    void refreshTable(1:arctic_commons.TableIdentifier tableIdentifier)
         throws(1: arctic_commons.OperationErrorException e);
}
