namespace java com.netease.arctic.ams.api

/**
* General definition of the arctic thrift interface.
* This file defines the type definitions that all of arctic's multiple thrift services depend on.
**/

exception AlreadyExistsException {
  1: string message
}

exception InvalidObjectException {
  1: string message
}

exception NoSuchObjectException {
  1: string message
}

exception MetaException {
  1: string message
}

exception NotSupportedException {
  1: string message
}

exception OperationConflictException {
  1: string message
}

exception ArcticException {
  1: i32 errorCode
  2: string errorName
  3: string message
}

struct TableIdentifier {
    1:string catalog;
    2:string database;
    3:string tableName;
}

// inner class begin

struct ColumnInfo {
    1:optional i32 id;
    2:string name;
    3:optional string type;
    4:optional string doc;
    5:bool isOptional;
}

struct Schema {
    1:list<ColumnInfo> columns;
    2:optional list<ColumnInfo> pks;
    3:optional list<ColumnInfo> partitionColumns;
    4:optional list<ColumnInfo> sortColumns;
}



