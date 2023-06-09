/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.hive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Order;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

@JsonTypeName("table")
public class HiveTableWrapper {

  @JsonIgnore
  private HiveTableWithColumnCache table;

  @JsonProperty
  public String tableName;
  @JsonProperty
  public String dbName;
  @JsonProperty
  public String owner;
  @JsonProperty
  public int createTime;
  @JsonProperty
  public int lastAccessTime;
  @JsonProperty
  public int retention;
  @JsonProperty
  public StorageDescriptorWrapper sd;
  @JsonProperty
  public List<FieldSchemaWrapper> partitionKeys;
  @JsonProperty
  public Map<String, String> parameters;
  @JsonProperty
  public String viewOriginalText;
  @JsonProperty
  public String viewExpandedText;
  @JsonProperty
  public String tableType;
  @JsonProperty
  public ColumnsCacheWrapper columnsCache;

  @JsonIgnore
  public final Map<String, String> partitionNameTypeMap = new HashMap<>();

  @JsonCreator
  public HiveTableWrapper(@JsonProperty("tableName") String tableName, @JsonProperty("dbName") String dbName, @JsonProperty("owner") String owner,
                          @JsonProperty("createTime") int createTime, @JsonProperty("lastAccessTime") int lastAccessTime,
                          @JsonProperty("retention") int retention, @JsonProperty("sd") StorageDescriptorWrapper sd,
                          @JsonProperty("partitionKeys") List<FieldSchemaWrapper> partitionKeys, @JsonProperty("parameters") Map<String, String> parameters,
                          @JsonProperty("viewOriginalText") String viewOriginalText, @JsonProperty("viewExpandedText") String viewExpandedText,
                          @JsonProperty("tableType") String tableType, @JsonProperty("columnsCache") ColumnsCacheWrapper columnsCache
  ) {
    this.tableName = tableName;
    this.dbName = dbName;
    this.owner = owner;
    this.createTime = createTime;
    this.lastAccessTime = lastAccessTime;
    this.retention = retention;
    this.sd = sd;
    this.partitionKeys = partitionKeys;
    this.parameters = parameters;
    this.viewOriginalText = viewOriginalText;
    this.viewExpandedText = viewExpandedText;
    this.tableType = tableType;
    this.columnsCache = columnsCache;

    List<FieldSchema> partitionKeysUnwrapped = Lists.newArrayList();
    for (FieldSchemaWrapper w : partitionKeys) {
      partitionKeysUnwrapped.add(w.getFieldSchema());
      partitionNameTypeMap.put(w.name, w.type);
    }
    StorageDescriptor sdUnwrapped = sd.getSd();
    this.table = new HiveTableWithColumnCache(tableName, dbName, owner, createTime, lastAccessTime, retention, sdUnwrapped, partitionKeysUnwrapped,
        parameters, viewOriginalText, viewExpandedText, tableType, columnsCache.getColumnListsCache());
  }

  public HiveTableWrapper(HiveTableWithColumnCache table) {
    if (table == null) {
      return;
    }
    this.table = table;
    this.tableName = table.getTableName();
    this.dbName = table.getDbName();
    this.owner = table.getOwner();
    this.createTime = table.getCreateTime();
    this.lastAccessTime = table.getLastAccessTime();
    this.retention = table.getRetention();
    this.sd = new StorageDescriptorWrapper(table.getSd());
    this.partitionKeys = Lists.newArrayList();
    for (FieldSchema f : table.getPartitionKeys()) {
      this.partitionKeys.add(new FieldSchemaWrapper(f));
      partitionNameTypeMap.put(f.getName(), f.getType());
    }
    this.parameters = table.getParameters();
    this.viewOriginalText = table.getViewOriginalText();
    this.viewExpandedText = table.getViewExpandedText();
    this.tableType = table.getTableType();
    this.columnsCache = new ColumnsCacheWrapper(table.getColumnListsCache());
  }

  @JsonIgnore
  public HiveTableWithColumnCache getTable() {
    return table;
  }

  @JsonIgnore
  public Map<String, String> getParameters() {
    return parameters;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Table(");

    sb.append("dbName:");
    sb.append(this.dbName);
    sb.append(", ");

    sb.append("tableName:");
    sb.append(this.tableName);
    sb.append(")");

    return sb.toString();
  }

  /**
   * Wrapper for {@link Partition} class. Used for serialization and deserialization of {@link HivePartition}.
   */
  public static class HivePartitionWrapper {

    @JsonIgnore
    private HivePartition partition;

    @JsonProperty
    public List<String> values;

    @JsonProperty
    public String tableName;

    @JsonProperty
    public String dbName;

    @JsonProperty
    public int createTime;

    @JsonProperty
    public int lastAccessTime;

    @JsonProperty
    public StorageDescriptorWrapper sd;

    @JsonProperty
    public Map<String, String> parameters;

    @JsonProperty
    private int columnListIndex;

    @JsonCreator
    public HivePartitionWrapper(@JsonProperty("values") List<String> values, @JsonProperty("tableName") String tableName,
                                @JsonProperty("dbName") String dbName, @JsonProperty("createTime") int createTime,
                                @JsonProperty("lastAccessTime") int lastAccessTime, @JsonProperty("sd") StorageDescriptorWrapper sd,
                                @JsonProperty("parameters") Map<String, String> parameters, @JsonProperty("columnListIndex") int columnListIndex) {
      this.values = values;
      this.tableName = tableName;
      this.dbName = dbName;
      this.createTime = createTime;
      this.lastAccessTime = lastAccessTime;
      this.sd = sd;
      this.parameters = parameters;
      this.columnListIndex = columnListIndex;

      StorageDescriptor sdUnwrapped = sd.getSd();
      this.partition = new HivePartition(values, tableName, dbName, createTime, lastAccessTime, sdUnwrapped, parameters, columnListIndex);
    }

    public HivePartitionWrapper(HivePartition partition) {
      if (partition == null) {
        return;
      }
      this.partition = partition;
      this.values = partition.getValues();
      this.tableName = partition.getTableName();
      this.dbName = partition.getDbName();
      this.createTime = partition.getCreateTime();
      this.lastAccessTime = partition.getLastAccessTime();
      this.sd = new StorageDescriptorWrapper(partition.getSd());
      this.parameters = partition.getParameters();
      this.columnListIndex = partition.getColumnListIndex();
    }

    @JsonIgnore
    public HivePartition getPartition() {
      return partition;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("Partition(");
      sb.append("values:");
      sb.append(this.values);
      sb.append(")");
      return sb.toString();
    }
  }

  /**
   * Wrapper for {@link StorageDescriptor} class.
   * Used in {@link HivePartitionWrapper} and {@link HiveTableWrapper}
   * for serialization and deserialization of {@link StorageDescriptor}.
   */
  public static class StorageDescriptorWrapper {

    @JsonIgnore
    private StorageDescriptor sd;

    // column lists stored in ColumnListsCache
    @JsonIgnore
    public List<FieldSchemaWrapper> columns;

    @JsonProperty
    public String location;

    @JsonProperty
    public String inputFormat;

    @JsonProperty
    public String outputFormat;

    @JsonProperty
    public boolean compressed;

    @JsonProperty
    public int numBuckets;

    @JsonProperty
    public SerDeInfoWrapper serDeInfo;

    @JsonProperty
    public List<OrderWrapper> sortCols;

    @JsonProperty
    public Map<String, String> parameters;

    @JsonCreator
    public StorageDescriptorWrapper(@JsonProperty("columns") List<FieldSchemaWrapper> columns, @JsonProperty("location") String location, @JsonProperty("inputFormat") String inputFormat,
                                    @JsonProperty("outputFormat") String outputFormat, @JsonProperty("compressed") boolean compressed, @JsonProperty("numBuckets") int numBuckets,
                                    @JsonProperty("serDeInfo") SerDeInfoWrapper serDeInfo,  @JsonProperty("sortCols") List<OrderWrapper> sortCols,
                                    @JsonProperty("parameters") Map<String,String> parameters) {
      this.columns = columns;
      this.location = location;
      this.inputFormat = inputFormat;
      this.outputFormat = outputFormat;
      this.compressed = compressed;
      this.numBuckets = numBuckets;
      this.serDeInfo = serDeInfo;
      this.sortCols = sortCols;
      this.parameters = parameters;
      List<FieldSchema> colsUnwrapped;
      if (columns != null) {
        colsUnwrapped = Lists.newArrayList();
        for (FieldSchemaWrapper fieldSchema : columns) {
          colsUnwrapped.add(fieldSchema.getFieldSchema());
        }
      } else {
        colsUnwrapped = null;
      }
      SerDeInfo serDeInfoUnwrapped = serDeInfo.getSerDeInfo();
      List<Order> sortColsUnwrapped;
      if (sortCols != null) {
        sortColsUnwrapped = Lists.newArrayList();
        for (OrderWrapper order : sortCols) {
          sortColsUnwrapped.add(order.getOrder());
        }
      } else {
        sortColsUnwrapped = null;
      }
      sd = new StorageDescriptor(colsUnwrapped, location, inputFormat, outputFormat,
        compressed, numBuckets, serDeInfoUnwrapped, null, sortColsUnwrapped, parameters);
    }

    public StorageDescriptorWrapper(StorageDescriptor storageDescriptor) {
      sd = storageDescriptor;
      location = storageDescriptor.getLocation();
      inputFormat = storageDescriptor.getInputFormat();
      outputFormat = storageDescriptor.getOutputFormat();
      compressed = storageDescriptor.isCompressed();
      numBuckets = storageDescriptor.getNumBuckets();
      serDeInfo = new SerDeInfoWrapper(storageDescriptor.getSerdeInfo());
      if (sd.getSortCols() != null) {
        sortCols = Lists.newArrayList();
        for (Order order : sd.getSortCols()) {
          sortCols.add(new OrderWrapper(order));
        }
      }
      parameters = storageDescriptor.getParameters();
      if (sd.getCols() != null) {
        this.columns = Lists.newArrayList();
        for (FieldSchema fieldSchema : sd.getCols()) {
          this.columns.add(new FieldSchemaWrapper(fieldSchema));
        }
      }
    }

    @JsonIgnore
    public StorageDescriptor getSd() {
      return sd;
    }
  }

  public static class SerDeInfoWrapper {
    @JsonIgnore
    private SerDeInfo serDeInfo;
    @JsonProperty
    public String name;
    @JsonProperty
    public String serializationLib;
    @JsonProperty
    public Map<String,String> parameters;

    @JsonCreator
    public SerDeInfoWrapper(@JsonProperty("name") String name, @JsonProperty("serializationLib") String serializationLib, @JsonProperty("parameters") Map<String, String> parameters) {
      this.name = name;
      this.serializationLib = serializationLib;
      this.parameters = parameters;
      this.serDeInfo = new SerDeInfo(name, serializationLib, parameters);
    }

    public SerDeInfoWrapper(SerDeInfo serDeInfo) {
      this.serDeInfo = serDeInfo;
      this.name = serDeInfo.getName();
      this.serializationLib = serDeInfo.getSerializationLib();
      this.parameters = serDeInfo.getParameters();
    }

    @JsonIgnore
    public SerDeInfo getSerDeInfo() {
      return serDeInfo;
    }
  }

  public static class FieldSchemaWrapper {
    @JsonIgnore
    private FieldSchema fieldSchema;
    @JsonProperty
    public String name;
    @JsonProperty
    public String type;
    @JsonProperty
    public String comment;

    @JsonCreator
    public FieldSchemaWrapper(@JsonProperty("name") String name, @JsonProperty("type") String type, @JsonProperty("comment") String comment) {
      this.name = name;
      this.type = type;
      this.comment = comment;
      this.fieldSchema = new FieldSchema(name, type, comment);
    }

    public FieldSchemaWrapper(FieldSchema fieldSchema) {
      this.fieldSchema = fieldSchema;
      this.name = fieldSchema.getName();
      this.type = fieldSchema.getType();
      this.comment = fieldSchema.getComment();
    }

    @JsonIgnore
    public FieldSchema getFieldSchema() {
      return fieldSchema;
    }
  }

  public static class OrderWrapper {
    @JsonIgnore
    private Order ord;
    @JsonProperty
    public String col;
    @JsonProperty
    public int order;

    @JsonCreator
    public OrderWrapper(@JsonProperty("col") String col, @JsonProperty("order") int order) {
      this.col = col;
      this.order = order;
    }

    public OrderWrapper(Order ord) {
      this.ord = ord;
      this.col = ord.getCol();
      this.order = ord.getOrder();
    }

    @JsonIgnore
    public Order getOrder() {
      return ord;
    }
  }

  public Map<String, String> getPartitionNameTypeMap() {
    return partitionNameTypeMap;
  }

  /**
   * Wrapper for {@link ColumnListsCache} class.
   * Used in {@link HiveTableWrapper} for serialization and deserialization of {@link ColumnListsCache}.
   */
  public static class ColumnsCacheWrapper {
    @JsonIgnore
    private final ColumnListsCache columnListsCache;

    @JsonProperty
    private final List<List<FieldSchemaWrapper>> keys;

    @JsonCreator
    public ColumnsCacheWrapper(@JsonProperty("keys") List<List<FieldSchemaWrapper>> keys) {
      this.keys = keys;
      this.columnListsCache = new ColumnListsCache();
      for (List<FieldSchemaWrapper> columns : keys) {
        final List<FieldSchema> columnsUnwrapped = Lists.newArrayList();
        for (FieldSchemaWrapper field : columns) {
          columnsUnwrapped.add(field.getFieldSchema());
        }
        columnListsCache.addOrGet(columnsUnwrapped);
      }
    }

    public ColumnsCacheWrapper(ColumnListsCache columnListsCache) {
      this.columnListsCache = columnListsCache;
      final List<List<FieldSchemaWrapper>> keysWrapped = Lists.newArrayList();
      for (List<FieldSchema> columns : columnListsCache.getFields()) {
        final List<FieldSchemaWrapper> columnsWrapped = Lists.newArrayList();
        for (FieldSchema field : columns) {
          columnsWrapped.add(new FieldSchemaWrapper(field));
        }
        keysWrapped.add(columnsWrapped);
      }
      this.keys = keysWrapped;
    }

    @JsonIgnore
    public ColumnListsCache getColumnListsCache() {
      return columnListsCache;
    }
  }
}
