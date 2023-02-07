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
package org.apache.drill.exec.store.iceberg.format;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.AbstractWriter;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.PlannerPhase;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.schema.SchemaProvider;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.PluginRulesProviderImpl;
import org.apache.drill.exec.store.StoragePluginRulesSupplier;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.dfs.FormatMatcher;
import org.apache.drill.exec.store.dfs.FormatPlugin;
import org.apache.drill.exec.store.iceberg.IcebergGroupScan;
import org.apache.drill.exec.store.iceberg.plan.IcebergPluginImplementor;
import org.apache.drill.exec.store.plan.rel.PluginRel;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class IcebergFormatPlugin implements FormatPlugin {

  private static final String ICEBERG_CONVENTION_PREFIX = "ICEBERG.";

  /**
   * Generator for format id values. Formats with the same name may be defined
   * in multiple storage plugins, so using the unique id within the convention name
   * to ensure the rule names will be unique for different plugin instances.
   */
  private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

  private final FileSystemConfig storageConfig;

  private final IcebergFormatPluginConfig config;

  private final Configuration fsConf;

  private final DrillbitContext context;

  private final String name;

  private final IcebergFormatMatcher matcher;

  private final StoragePluginRulesSupplier storagePluginRulesSupplier;

  public IcebergFormatPlugin(
    String name,
    DrillbitContext context,
    Configuration fsConf,
    FileSystemConfig storageConfig,
    IcebergFormatPluginConfig config) {
    this.storageConfig = storageConfig;
    this.config = config;
    this.fsConf = fsConf;
    this.context = context;
    this.name = name;
    this.matcher = new IcebergFormatMatcher(this);
    this.storagePluginRulesSupplier = storagePluginRulesSupplier(name + NEXT_ID.getAndIncrement());
  }

  private static StoragePluginRulesSupplier storagePluginRulesSupplier(String name) {
    Convention convention = new Convention.Impl(ICEBERG_CONVENTION_PREFIX + name, PluginRel.class);
    return StoragePluginRulesSupplier.builder()
      .rulesProvider(new PluginRulesProviderImpl(convention, IcebergPluginImplementor::new))
      .supportsFilterPushdown(true)
      .supportsProjectPushdown(true)
      .supportsLimitPushdown(true)
      .convention(convention)
      .build();
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public boolean supportsWrite() {
    return false;
  }

  @Override
  public boolean supportsAutoPartitioning() {
    return false;
  }

  @Override
  public FormatMatcher getMatcher() {
    return matcher;
  }

  @Override
  public AbstractWriter getWriter(PhysicalOperator child, String location, List<String> partitionColumns) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<? extends RelOptRule> getOptimizerRules(PlannerPhase phase) {
    switch (phase) {
      case PHYSICAL:
      case LOGICAL:
        return storagePluginRulesSupplier.getOptimizerRules();
      case LOGICAL_PRUNE_AND_JOIN:
      case LOGICAL_PRUNE:
      case PARTITION_PRUNING:
      case JOIN_PLANNING:
      default:
        return Collections.emptySet();
    }
  }

  @Override
  public AbstractGroupScan getGroupScan(String userName, FileSelection selection, List<SchemaPath> columns) throws IOException {
    return IcebergGroupScan.builder()
      .userName(userName)
      .formatPlugin(this)
      .path(getPath(selection))
      .columns(columns)
      .maxRecords(-1)
      .build();
  }

  @Override
  public AbstractGroupScan getGroupScan(String userName, FileSelection selection,
    List<SchemaPath> columns, MetadataProviderManager metadataProviderManager) throws IOException {
    SchemaProvider schemaProvider = metadataProviderManager.getSchemaProvider();
    TupleMetadata schema = schemaProvider != null
      ? schemaProvider.read().getSchema()
      : null;
    return IcebergGroupScan.builder()
      .userName(userName)
      .formatPlugin(this)
      .schema(schema)
      .path(getPath(selection))
      .columns(columns)
      .maxRecords(-1)
      .build();
  }

  @Override
  public boolean supportsStatistics() {
    return false;
  }

  @Override
  public DrillStatsTable.TableStatistics readStatistics(FileSystem fs, Path statsTablePath) {
    throw new UnsupportedOperationException("unimplemented");
  }

  @Override
  public void writeStatistics(DrillStatsTable.TableStatistics statistics, FileSystem fs, Path statsTablePath) {
    throw new UnsupportedOperationException("unimplemented");
  }

  @Override
  public IcebergFormatPluginConfig getConfig() {
    return config;
  }

  @Override
  public FileSystemConfig getStorageConfig() {
    return storageConfig;
  }

  @Override
  public Configuration getFsConf() {
    return fsConf;
  }

  @Override
  public DrillbitContext getContext() {
    return context;
  }

  @Override
  public String getName() {
    return name;
  }

  public Convention getConvention() {
    return storagePluginRulesSupplier.convention();
  }

  private String getPath(FileSelection selection) {
    String path = selection.selectionRoot.toUri().getPath();
    if (selection instanceof IcebergMetadataFileSelection) {
      IcebergMetadataFileSelection metadataFileSelection = (IcebergMetadataFileSelection) selection;
      path = String.format("%s%s%s", path, IcebergFormatLocationTransformer.METADATA_SEPARATOR,
        metadataFileSelection.getMetadataTableType().name().toLowerCase());
    }
    return path;
  }
}
