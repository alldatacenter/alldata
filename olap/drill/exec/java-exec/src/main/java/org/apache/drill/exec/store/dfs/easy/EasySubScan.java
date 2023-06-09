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
package org.apache.drill.exec.store.dfs.easy;

import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.AbstractSubScan;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.schedule.CompleteFileWork.FileWorkImpl;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.hadoop.fs.Path;


@JsonTypeName("fs-sub-scan")
public class EasySubScan extends AbstractSubScan {

  private final List<FileWorkImpl> files;
  private final EasyFormatPlugin<?> formatPlugin;
  private final List<SchemaPath> columns;
  private final Path selectionRoot;
  private final int partitionDepth;
  private final TupleMetadata schema;
  // Defined as -1 means no limit, 0 means schema only, >0 is a limit.
  private final int limit;

  @JsonCreator
  public EasySubScan(
    @JsonProperty("userName") String userName,
    @JsonProperty("files") List<FileWorkImpl> files,
    @JsonProperty("storage") StoragePluginConfig storageConfig,
    @JsonProperty("format") FormatPluginConfig formatConfig,
    @JacksonInject StoragePluginRegistry engineRegistry,
    @JsonProperty("columns") List<SchemaPath> columns,
    @JsonProperty("selectionRoot") Path selectionRoot,
    @JsonProperty("partitionDepth") int partitionDepth,
    @JsonProperty("schema") TupleMetadata schema,
    @JsonProperty("limit") int limit
    ) throws ExecutionSetupException {
    super(userName);
    this.formatPlugin = engineRegistry.resolveFormat(storageConfig, formatConfig, EasyFormatPlugin.class);
    this.files = files;
    this.columns = columns;
    this.selectionRoot = selectionRoot;
    this.partitionDepth = partitionDepth;
    this.schema = schema;
    this.limit = limit;
  }

  public EasySubScan(String userName, List<FileWorkImpl> files, EasyFormatPlugin<?> plugin,
      List<SchemaPath> columns, Path selectionRoot, int partitionDepth, TupleMetadata schema, int limit) {
    super(userName);
    this.formatPlugin = plugin;
    this.files = files;
    this.columns = columns;
    this.selectionRoot = selectionRoot;
    this.partitionDepth = partitionDepth;
    this.schema = schema;
    this.limit = limit;
  }

  @JsonProperty
  public Path getSelectionRoot() { return selectionRoot; }

  @JsonProperty
  public int getPartitionDepth() { return partitionDepth; }

  @JsonIgnore
  public EasyFormatPlugin<?> getFormatPlugin() { return formatPlugin; }

  @JsonProperty("files")
  public List<FileWorkImpl> getWorkUnits() { return files; }

  @JsonProperty("storage")
  public StoragePluginConfig getStorageConfig() { return formatPlugin.getStorageConfig(); }

  @JsonProperty("format")
  public FormatPluginConfig getFormatConfig() { return formatPlugin.getConfig(); }

  @JsonProperty("columns")
  public List<SchemaPath> getColumns() { return columns; }

  @JsonProperty("schema")
  public TupleMetadata getSchema() { return schema; }

  @JsonProperty("limit")
  public int getLimit() { return limit; }

  // Shim method to return the original, incorrect encoding of the limit that
  // used maxRecords = 0 to mean unlimited. LIMIT 0 is perfectly valid.
  @JsonIgnore
  public int getMaxRecords() {
    if (limit < 0) {
      // Previous version used 0 = no limit
      return 0;
    }
    if (limit == 0) {
      // Previous version didn't support LIMIT 0
      return 1;
    }
    return limit;
  }
  @Override
  public String getOperatorType() { return formatPlugin.getReaderOperatorType(); }
}
