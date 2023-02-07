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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Table;

/**
 * The class represents "cache" for partition and table columns.
 * Used to reduce physical plan for Hive tables.
 * Only unique partition lists of columns stored in the column lists cache.
 * Table columns should be stored at index 0.
 */
public class ColumnListsCache {
  // contains immutable column lists
  private final List<List<FieldSchema>> fields;

  // keys of the map are column lists and values are them positions in list fields
  private final Map<List<FieldSchema>, Integer> keys;

  public ColumnListsCache(Table table) {
    this();
    // table columns stored at index 0.
    addOrGet(table.getSd().getCols());
  }

  public ColumnListsCache() {
    this.fields = new ArrayList<>();
    this.keys = new HashMap<>();
  }

  /**
   * Checks if column list has been added before and returns position of column list.
   * If list is unique, adds list to the fields list and returns it position.
   * Returns -1, if {@param columns} equals null.
   *
   * @param columns list of columns
   * @return index of {@param columns} or -1, if {@param columns} equals null
   */
  public int addOrGet(List<FieldSchema> columns) {
    if (columns == null) {
      return -1;
    }
    Integer index = keys.get(columns);
    if (index != null) {
      return index;
    } else {
      index = fields.size();
      final List<FieldSchema> immutableList = ImmutableList.copyOf(columns);
      fields.add(immutableList);
      keys.put(immutableList, index);
      return index;
    }
  }

  /**
   * Returns list of columns at the specified position in fields list,
   * or null if index is negative or greater than fields list size.
   *
   * @param index index of column list to return
   * @return list of columns at the specified position in fields list
   * or null if index is negative or greater than fields list size
   */
  public List<FieldSchema> getColumns(int index) {
   return (index > -1 && index < fields.size()) ? fields.get(index) : null;
  }

  /**
   * Safely retrieves Hive table columns from cache.
   *
   * @return list of table columns defined in hive
   */
  public List<FieldSchema> getTableSchemaColumns() {
    List<FieldSchema> tableSchemaColumns = getColumns(0);
    Preconditions.checkNotNull(tableSchemaColumns, "Failed to get columns for Hive table from cache.");
    return tableSchemaColumns;
  }

  public List<List<FieldSchema>> getFields() {
    return new ArrayList<>(fields);
  }

}
