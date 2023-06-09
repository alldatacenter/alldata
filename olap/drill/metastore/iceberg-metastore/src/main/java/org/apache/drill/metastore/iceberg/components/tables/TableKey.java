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
package org.apache.drill.metastore.iceberg.components.tables;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.hadoop.fs.Path;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Is used to uniquely identify Drill table in Metastore Tables component
 * based on storage plugin, workspace and table name.
 */
public class TableKey {

  private final String storagePlugin;
  private final String workspace;
  private final String tableName;

  public TableKey(String storagePlugin, String workspace, String tableName) {
    this.storagePlugin = storagePlugin;
    this.workspace = workspace;
    this.tableName = tableName;
  }

  public static TableKey of(TableMetadataUnit unit) {
    return new TableKey(unit.storagePlugin(), unit.workspace(), unit.tableName());
  }

  public String storagePlugin() {
    return storagePlugin;
  }

  public String workspace() {
    return workspace;
  }

  public String tableName() {
    return tableName;
  }

  /**
   * Constructs table location based on given Iceberg table location.
   * For example, metadata for the table dfs.tmp.nation will be stored in
   * [METASTORE_TABLES_ROOT_DIRECTORY]/dfs/tmp/nation folder.
   *
   * @param base Iceberg table location
   * @return table location
   */
  public String toLocation(String base) {
    Path path = new Path(base);
    path = new Path(path, storagePlugin);
    path = new Path(path, workspace);
    path = new Path(path, tableName);
    return path.toUri().getPath();
  }

  /**
   * Convert table key data into filter conditions.
   *
   * @return map of with condition references anf values
   */
  public Map<MetastoreColumn, Object> toFilterConditions() {
    Map<MetastoreColumn, Object> conditions = new HashMap<>();
    conditions.put(MetastoreColumn.STORAGE_PLUGIN, storagePlugin);
    conditions.put(MetastoreColumn.WORKSPACE, workspace);
    conditions.put(MetastoreColumn.TABLE_NAME, tableName);
    return conditions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(storagePlugin, workspace, tableName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TableKey that = (TableKey) o;
    return Objects.equals(storagePlugin, that.storagePlugin)
      && Objects.equals(workspace, that.workspace)
      && Objects.equals(tableName, that.tableName);
  }
}
