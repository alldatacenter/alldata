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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.exec.metastore.store.FileSystemMetadataProviderManager;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.parquet.ParquetReaderConfig;
import org.apache.drill.metastore.metadata.LocationProvider;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.ReadEntryWithPath;
import org.apache.drill.exec.store.hive.HiveMetadataProvider.LogicalInputSplit;
import org.apache.drill.exec.store.parquet.AbstractParquetGroupScan;
import org.apache.drill.exec.store.parquet.RowGroupReadEntry;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@JsonTypeName("hive-drill-native-parquet-scan")
public class HiveDrillNativeParquetScan extends AbstractParquetGroupScan {

  private final HiveStoragePlugin hiveStoragePlugin;
  private final HivePartitionHolder hivePartitionHolder;
  private final Map<String, String> confProperties;

  @JsonCreator
  public HiveDrillNativeParquetScan(@JacksonInject StoragePluginRegistry engineRegistry,
                                    @JsonProperty("userName") String userName,
                                    @JsonProperty("hiveStoragePluginConfig") HiveStoragePluginConfig hiveStoragePluginConfig,
                                    @JsonProperty("columns") List<SchemaPath> columns,
                                    @JsonProperty("entries") List<ReadEntryWithPath> entries,
                                    @JsonProperty("hivePartitionHolder") HivePartitionHolder hivePartitionHolder,
                                    @JsonProperty("confProperties") Map<String, String> confProperties,
                                    @JsonProperty("readerConfig") ParquetReaderConfig readerConfig,
                                    @JsonProperty("filter") LogicalExpression filter,
                                    @JsonProperty("schema") TupleMetadata schema) throws IOException {
    super(ImpersonationUtil.resolveUserName(userName), columns, entries, readerConfig, filter);
    this.hiveStoragePlugin = engineRegistry.resolve(hiveStoragePluginConfig, HiveStoragePlugin.class);
    this.confProperties = confProperties;

    HiveParquetTableMetadataProvider.Builder builder =
        defaultTableMetadataProviderBuilder(new FileSystemMetadataProviderManager());

    HiveParquetTableMetadataProvider metadataProvider = builder
        .withEntries(entries)
        .withHivePartitionHolder(hivePartitionHolder)
        .withHiveStoragePlugin(hiveStoragePlugin)
        .withReaderConfig(readerConfig)
        .withSchema(schema)
        .build();

    this.metadataProvider = metadataProvider;
    this.hivePartitionHolder = metadataProvider.getHivePartitionHolder();
    this.fileSet = metadataProvider.getFileSet();

    init();
  }

  public HiveDrillNativeParquetScan(String userName,
                                    List<SchemaPath> columns,
                                    HiveStoragePlugin hiveStoragePlugin,
                                    List<LogicalInputSplit> logicalInputSplits,
                                    Map<String, String> confProperties,
                                    ParquetReaderConfig readerConfig) throws IOException {
    this(userName, columns, hiveStoragePlugin, logicalInputSplits, confProperties, readerConfig, ValueExpressions.BooleanExpression.TRUE);
  }

  public HiveDrillNativeParquetScan(String userName,
                                    List<SchemaPath> columns,
                                    HiveStoragePlugin hiveStoragePlugin,
                                    List<LogicalInputSplit> logicalInputSplits,
                                    Map<String, String> confProperties,
                                    ParquetReaderConfig readerConfig,
                                    LogicalExpression filter) throws IOException {
    super(userName, columns, new ArrayList<>(), readerConfig, filter);

    this.hiveStoragePlugin = hiveStoragePlugin;
    this.confProperties = confProperties;

    HiveParquetTableMetadataProvider.Builder builder =
        tableMetadataProviderBuilder(new FileSystemMetadataProviderManager());

    HiveParquetTableMetadataProvider metadataProvider = builder
        .withHiveStoragePlugin(hiveStoragePlugin)
        .withLogicalInputSplits(logicalInputSplits)
        .withReaderConfig(readerConfig)
        .build();

    this.metadataProvider = metadataProvider;
    this.entries = metadataProvider.getEntries();
    this.hivePartitionHolder = metadataProvider.getHivePartitionHolder();
    this.fileSet = metadataProvider.getFileSet();

    init();
  }

  /**
   * Copy constructor for shallow partial cloning
   * @param that old groupScan
   */
  private HiveDrillNativeParquetScan(HiveDrillNativeParquetScan that) {
    super(that);
    this.hiveStoragePlugin = that.hiveStoragePlugin;
    this.hivePartitionHolder = that.hivePartitionHolder;
    this.confProperties = that.confProperties;
  }

  @JsonProperty
  public HiveStoragePluginConfig getHiveStoragePluginConfig() {
    return hiveStoragePlugin.getConfig();
  }

  @JsonProperty
  public HivePartitionHolder getHivePartitionHolder() {
    return hivePartitionHolder;
  }

  @JsonProperty
  public Map<String, String> getConfProperties() {
    return confProperties;
  }

  @Override
  public SubScan getSpecificScan(int minorFragmentId) {
    List<RowGroupReadEntry> readEntries = getReadEntries(minorFragmentId);
    HivePartitionHolder subPartitionHolder = new HivePartitionHolder();
    for (RowGroupReadEntry readEntry : readEntries) {
      List<String> values = hivePartitionHolder.get(readEntry.getPath());
      subPartitionHolder.add(readEntry.getPath(), values);
    }
    return new HiveDrillNativeParquetRowGroupScan(getUserName(), hiveStoragePlugin, readEntries, columns, subPartitionHolder,
      confProperties, readerConfig, filter, getTableMetadata().getSchema());
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new HiveDrillNativeParquetScan(this);
  }

  @Override
  public HiveDrillNativeParquetScan clone(FileSelection selection) throws IOException {
    HiveDrillNativeParquetScan newScan = new HiveDrillNativeParquetScan(this);
    newScan.modifyFileSelection(selection);
    newScan.init();
    return newScan;
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    HiveDrillNativeParquetScan newScan = new HiveDrillNativeParquetScan(this);
    newScan.columns = columns;
    return newScan;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("HiveDrillNativeParquetScan [");
    builder.append("entries=").append(entries);
    builder.append(", numFiles=").append(getEntries().size());
    builder.append(", numRowGroups=").append(getRowGroupsMetadata().size());

    String filterString = getFilterString();
    if (!filterString.isEmpty()) {
      builder.append(", filter=").append(filterString);
    }

    builder.append(", columns=").append(columns);
    builder.append("]");

    return builder.toString();
  }

  @Override
  protected RowGroupScanFilterer<?> getFilterer() {
    return new HiveDrillNativeParquetScanFilterer(this);
  }

  @Override
  protected Collection<CoordinationProtos.DrillbitEndpoint> getDrillbits() {
    return hiveStoragePlugin.getContext().getBits();
  }

  @Override
  protected AbstractParquetGroupScan cloneWithFileSelection(Collection<Path> filePaths) throws IOException {
    FileSelection newSelection = new FileSelection(null, new ArrayList<>(filePaths), null, null, false);
    return clone(newSelection);
  }

  @Override
  protected boolean supportsFileImplicitColumns() {
    // current group scan should populate directory partition values
    return true;
  }

  @Override
  protected List<String> getPartitionValues(LocationProvider locationProvider) {
    return hivePartitionHolder.get(locationProvider.getPath());
  }

  @Override
  protected HiveParquetTableMetadataProvider.Builder tableMetadataProviderBuilder(MetadataProviderManager source) {
    return defaultTableMetadataProviderBuilder(source);
  }

  @Override
  protected HiveParquetTableMetadataProvider.Builder defaultTableMetadataProviderBuilder(MetadataProviderManager source) {
    return new HiveParquetTableMetadataProvider.Builder(source);
  }

  /**
   * Implementation of RowGroupScanFilterer which uses {@link HiveDrillNativeParquetScanFilterer} as source and
   * builds {@link HiveDrillNativeParquetScanFilterer} instance with filtered metadata.
   */
  private static class HiveDrillNativeParquetScanFilterer extends RowGroupScanFilterer<HiveDrillNativeParquetScanFilterer> {

    public HiveDrillNativeParquetScanFilterer(HiveDrillNativeParquetScan source) {
      super(source);
    }

    @Override
    protected AbstractParquetGroupScan getNewScan() {
      return new HiveDrillNativeParquetScan((HiveDrillNativeParquetScan) source);
    }

    @Override
    protected HiveDrillNativeParquetScanFilterer self() {
      return this;
    }
  }
}
