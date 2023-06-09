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

import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;

import java.util.List;
import java.util.Map;

/**
 * This class is wrapper of {@link Table} class and used for
 * storage of such additional information as column lists cache.
 */
@SuppressWarnings("serial")
public class HiveTableWithColumnCache extends Table {

  private ColumnListsCache columnListsCache;

  public HiveTableWithColumnCache() {
    super();
  }

  public HiveTableWithColumnCache(
    String tableName,
    String dbName,
    String owner,
    int createTime,
    int lastAccessTime,
    int retention,
    StorageDescriptor sd,
    List<FieldSchema> partitionKeys,
    Map<String,String> parameters,
    String viewOriginalText,
    String viewExpandedText,
    String tableType,
    ColumnListsCache columnListsCache) {
    super(tableName, dbName, owner, createTime, lastAccessTime, retention, sd,
      partitionKeys, parameters, viewOriginalText, viewExpandedText, tableType);
    this.columnListsCache = columnListsCache;
  }

  public HiveTableWithColumnCache(HiveTableWithColumnCache other) {
    super(other);
    columnListsCache = other.getColumnListsCache();
  }

  public HiveTableWithColumnCache(Table other, ColumnListsCache columnListsCache) {
    super(other);
    this.columnListsCache = columnListsCache;
  }

  /**
   * To reduce physical plan for Hive tables, unique partition lists of columns stored in the
   * table's column lists cache.
   *
   * @return table's column lists cache
   */
  public ColumnListsCache getColumnListsCache() {
    return columnListsCache;
  }
}
