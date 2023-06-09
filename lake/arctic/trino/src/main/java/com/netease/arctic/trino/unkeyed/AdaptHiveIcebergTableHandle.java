/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.trino.unkeyed;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.airlift.units.DataSize;
import io.trino.plugin.iceberg.IcebergColumnHandle;
import io.trino.plugin.iceberg.IcebergTableHandle;
import io.trino.plugin.iceberg.TableType;
import io.trino.spi.connector.RetryMode;
import io.trino.spi.predicate.TupleDomain;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class AdaptHiveIcebergTableHandle extends IcebergTableHandle {

  private final String schemaName;
  private final String tableName;
  private final TableType tableType;
  private final Optional<Long> snapshotId;
  private final String tableSchemaJson;
  private final int formatVersion;
  private final String tableLocation;
  private final Map<String, String> storageProperties;
  private final RetryMode retryMode;

  // Filter used during split generation and table scan, but not required to be strictly enforced by Iceberg Connector
  private final TupleDomain<IcebergColumnHandle> unenforcedPredicate;

  // Filter guaranteed to be enforced by Iceberg connector
  private final TupleDomain<IcebergColumnHandle> enforcedPredicate;

  private final Set<IcebergColumnHandle> projectedColumns;
  private final Optional<String> nameMappingJson;

  // OPTIMIZE only. Coordinator-only
  private final boolean recordScannedFiles;
  private final Optional<DataSize> maxScannedFileSize;

  @JsonCreator
  public AdaptHiveIcebergTableHandle(
      @JsonProperty("schemaName") String schemaName,
      @JsonProperty("tableName") String tableName,
      @JsonProperty("tableType") TableType tableType,
      @JsonProperty("snapshotId") Optional<Long> snapshotId,
      @JsonProperty("tableSchemaJson") String tableSchemaJson,
      @JsonProperty("formatVersion") int formatVersion,
      @JsonProperty("unenforcedPredicate") TupleDomain<IcebergColumnHandle> unenforcedPredicate,
      @JsonProperty("enforcedPredicate") TupleDomain<IcebergColumnHandle> enforcedPredicate,
      @JsonProperty("projectedColumns") Set<IcebergColumnHandle> projectedColumns,
      @JsonProperty("nameMappingJson") Optional<String> nameMappingJson,
      @JsonProperty("tableLocation") String tableLocation,
      @JsonProperty("storageProperties") Map<String, String> storageProperties,
      @JsonProperty("retryMode") RetryMode retryMode) {
    super(
        schemaName,
        tableName,
        tableType,
        snapshotId,
        tableSchemaJson,
        formatVersion,
        unenforcedPredicate,
        enforcedPredicate,
        projectedColumns,
        nameMappingJson,
        tableLocation,
        storageProperties,
        retryMode);
    this.schemaName = requireNonNull(schemaName, "schemaName is null");
    this.tableName = requireNonNull(tableName, "tableName is null");
    this.tableType = requireNonNull(tableType, "tableType is null");
    this.snapshotId = requireNonNull(snapshotId, "snapshotId is null");
    this.tableSchemaJson = requireNonNull(tableSchemaJson, "schemaJson is null");
    this.formatVersion = formatVersion;
    this.unenforcedPredicate = requireNonNull(unenforcedPredicate, "unenforcedPredicate is null");
    this.enforcedPredicate = requireNonNull(enforcedPredicate, "enforcedPredicate is null");
    this.projectedColumns = ImmutableSet.copyOf(requireNonNull(projectedColumns, "projectedColumns is null"));
    this.nameMappingJson = requireNonNull(nameMappingJson, "nameMappingJson is null");
    this.tableLocation = requireNonNull(tableLocation, "tableLocation is null");
    this.storageProperties = ImmutableMap.copyOf(requireNonNull(storageProperties, "storageProperties is null"));
    this.retryMode = requireNonNull(retryMode, "retryMode is null");
    recordScannedFiles = false;
    maxScannedFileSize = Optional.empty();
  }

  public AdaptHiveIcebergTableHandle(
      String schemaName,
      String tableName,
      TableType tableType,
      Optional<Long> snapshotId,
      String tableSchemaJson,
      int formatVersion,
      TupleDomain<IcebergColumnHandle> unenforcedPredicate,
      TupleDomain<IcebergColumnHandle> enforcedPredicate,
      Set<IcebergColumnHandle> projectedColumns,
      Optional<String> nameMappingJson,
      String tableLocation,
      Map<String, String> storageProperties,
      RetryMode retryMode,
      boolean recordScannedFiles,
      Optional<DataSize> maxScannedFileSize) {
    super(
        schemaName,
        tableName,
        tableType,
        snapshotId,
        tableSchemaJson,
        formatVersion,
        unenforcedPredicate,
        enforcedPredicate,
        projectedColumns,
        nameMappingJson,
        tableLocation,
        storageProperties,
        retryMode,
        recordScannedFiles,
        maxScannedFileSize);
    this.schemaName = requireNonNull(schemaName, "schemaName is null");
    this.tableName = requireNonNull(tableName, "tableName is null");
    this.tableType = requireNonNull(tableType, "tableType is null");
    this.snapshotId = requireNonNull(snapshotId, "snapshotId is null");
    this.tableSchemaJson = requireNonNull(tableSchemaJson, "schemaJson is null");
    this.formatVersion = formatVersion;
    this.unenforcedPredicate = requireNonNull(unenforcedPredicate, "unenforcedPredicate is null");
    this.enforcedPredicate = requireNonNull(enforcedPredicate, "enforcedPredicate is null");
    this.projectedColumns = ImmutableSet.copyOf(requireNonNull(projectedColumns, "projectedColumns is null"));
    this.nameMappingJson = requireNonNull(nameMappingJson, "nameMappingJson is null");
    this.tableLocation = requireNonNull(tableLocation, "tableLocation is null");
    this.storageProperties = ImmutableMap.copyOf(requireNonNull(storageProperties, "storageProperties is null"));
    this.retryMode = requireNonNull(retryMode, "retryMode is null");
    this.recordScannedFiles = recordScannedFiles;
    this.maxScannedFileSize = requireNonNull(maxScannedFileSize, "maxScannedFileSize is null");
  }


  public AdaptHiveIcebergTableHandle withProjectedColumns(Set<IcebergColumnHandle> projectedColumns) {
    return new AdaptHiveIcebergTableHandle(
        schemaName,
        tableName,
        tableType,
        snapshotId,
        tableSchemaJson,
        formatVersion,
        unenforcedPredicate,
        enforcedPredicate,
        projectedColumns,
        nameMappingJson,
        tableLocation,
        storageProperties,
        retryMode,
        recordScannedFiles,
        maxScannedFileSize);
  }

  public AdaptHiveIcebergTableHandle withRetryMode(RetryMode retryMode) {
    return new AdaptHiveIcebergTableHandle(
        schemaName,
        tableName,
        tableType,
        snapshotId,
        tableSchemaJson,
        formatVersion,
        unenforcedPredicate,
        enforcedPredicate,
        projectedColumns,
        nameMappingJson,
        tableLocation,
        storageProperties,
        retryMode,
        recordScannedFiles,
        maxScannedFileSize);
  }

  public AdaptHiveIcebergTableHandle forOptimize(boolean recordScannedFiles, DataSize maxScannedFileSize) {
    return new AdaptHiveIcebergTableHandle(
        schemaName,
        tableName,
        tableType,
        snapshotId,
        tableSchemaJson,
        formatVersion,
        unenforcedPredicate,
        enforcedPredicate,
        projectedColumns,
        nameMappingJson,
        tableLocation,
        storageProperties,
        retryMode,
        recordScannedFiles,
        Optional.of(maxScannedFileSize));
  }
}
