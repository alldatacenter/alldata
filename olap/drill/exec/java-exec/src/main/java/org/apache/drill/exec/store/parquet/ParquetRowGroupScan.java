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
package org.apache.drill.exec.store.parquet;

import java.util.LinkedList;
import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.StoragePluginRegistry;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

// Class containing information for reading a single parquet row group from HDFS
@JsonTypeName("parquet-row-group-scan")
public class ParquetRowGroupScan extends AbstractParquetRowGroupScan {

  public static final String OPERATOR_TYPE = "PARQUET_ROW_GROUP_SCAN";

  private final ParquetFormatPlugin formatPlugin;
  private final ParquetFormatConfig formatConfig;

  @JsonCreator
  public ParquetRowGroupScan(@JacksonInject StoragePluginRegistry registry,
                             @JsonProperty("userName") String userName,
                             @JsonProperty("storageConfig") StoragePluginConfig storageConfig,
                             @JsonProperty("formatConfig") FormatPluginConfig formatConfig,
                             @JsonProperty("rowGroupReadEntries") LinkedList<RowGroupReadEntry> rowGroupReadEntries,
                             @JsonProperty("columns") List<SchemaPath> columns,
                             @JsonProperty("readerConfig") ParquetReaderConfig readerConfig,
                             @JsonProperty("selectionRoot") Path selectionRoot,
                             @JsonProperty("filter") LogicalExpression filter,
                             @JsonProperty("schema") TupleMetadata schema) throws ExecutionSetupException {
    this(userName,
        registry.resolveFormat(storageConfig, formatConfig, ParquetFormatPlugin.class),
        rowGroupReadEntries,
        columns,
        readerConfig,
        selectionRoot,
        filter,
        schema);
  }

  public ParquetRowGroupScan(String userName,
                             ParquetFormatPlugin formatPlugin,
                             List<RowGroupReadEntry> rowGroupReadEntries,
                             List<SchemaPath> columns,
                             ParquetReaderConfig readerConfig,
                             Path selectionRoot,
                             LogicalExpression filter,
                             TupleMetadata schema) {
    super(userName, rowGroupReadEntries, columns, readerConfig, filter, selectionRoot, schema);
    this.formatPlugin = Preconditions.checkNotNull(formatPlugin, "Could not find format config for the given configuration");
    this.formatConfig = formatPlugin.getConfig();
  }

  @JsonProperty
  public StoragePluginConfig getStorageConfig() {
    return formatPlugin.getStorageConfig();
  }

  @JsonProperty
  public ParquetFormatConfig getFormatConfig() {
    return formatConfig;
  }

  @JsonIgnore
  public ParquetFormatPlugin getStorageEngine() {
    return formatPlugin;
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new ParquetRowGroupScan(getUserName(), formatPlugin, rowGroupReadEntries, columns, readerConfig, selectionRoot, filter, schema);
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  @Override
  public AbstractParquetRowGroupScan copy(List<SchemaPath> columns) {
    return new ParquetRowGroupScan(getUserName(), formatPlugin, rowGroupReadEntries, columns, readerConfig, selectionRoot, filter, schema);
  }

  @Override
  public Configuration getFsConf(RowGroupReadEntry rowGroupReadEntry) {
    return formatPlugin.getFsConf();
  }

  @Override
  public boolean supportsFileImplicitColumns() {
    return selectionRoot != null;
  }

  @Override
  public List<String> getPartitionValues(RowGroupReadEntry rowGroupReadEntry) {
    return ColumnExplorer.listPartitionValues(rowGroupReadEntry.getPath(), selectionRoot, false);
  }
}

