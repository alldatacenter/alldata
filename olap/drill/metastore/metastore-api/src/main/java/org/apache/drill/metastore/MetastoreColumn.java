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
package org.apache.drill.metastore;

import org.apache.drill.metastore.exceptions.MetastoreException;

import java.util.stream.Stream;

/**
 * Metastore column definition, contains all Metastore column and their name
 * to unique their usage in the code.
 */
public enum MetastoreColumn {

  STORAGE_PLUGIN("storagePlugin"),
  WORKSPACE("workspace"),
  TABLE_NAME("tableName"),
  OWNER("owner"),
  TABLE_TYPE("tableType"),
  METADATA_TYPE("metadataType"),
  METADATA_KEY("metadataKey"),
  LOCATION("location"),
  INTERESTING_COLUMNS("interestingColumns"),
  SCHEMA("schema"),
  COLUMNS_STATISTICS("columnsStatistics"),
  METADATA_STATISTICS("metadataStatistics"),
  LAST_MODIFIED_TIME("lastModifiedTime"),
  PARTITION_KEYS("partitionKeys"),
  ADDITIONAL_METADATA("additionalMetadata"),
  METADATA_IDENTIFIER("metadataIdentifier"),
  COLUMN("column"),
  LOCATIONS("locations"),
  PARTITION_VALUES("partitionValues"),
  PATH("path"),
  ROW_GROUP_INDEX("rowGroupIndex"),
  HOST_AFFINITY("hostAffinity");

  private final String columnName;

  MetastoreColumn(String columnName) {
    this.columnName = columnName;
  }

  public String columnName() {
    return columnName;
  }

  /**
   * Looks up {@link MetastoreColumn} value for the given column name.
   *
   * @param columnName column name
   * @return {@link MetastoreColumn} value
   * @throws MetastoreException if {@link MetastoreColumn} value is not found
   */
  public static MetastoreColumn of(String columnName) {
    return Stream.of(MetastoreColumn.values())
      .filter(column -> column.columnName.equals(columnName))
      // we don't expect duplicates by column name in the enum
      .findAny()
      .orElseThrow(() -> new MetastoreException(String.format("Column with name [%s] is absent.", columnName)));
  }
}
