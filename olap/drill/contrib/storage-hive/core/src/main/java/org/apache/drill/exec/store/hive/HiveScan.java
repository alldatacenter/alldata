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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.ScanStats.GroupScanProperty;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.hive.HiveMetadataProvider.HiveStats;
import org.apache.drill.exec.store.hive.HiveMetadataProvider.LogicalInputSplit;
import org.apache.drill.exec.store.hive.HiveTableWrapper.HivePartitionWrapper;
import org.apache.drill.exec.util.Utilities;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import static org.apache.drill.exec.store.hive.HiveUtilities.createPartitionWithSpecColumns;

@JsonTypeName("hive-scan")
public class HiveScan extends AbstractGroupScan {
  private static final Logger logger = LoggerFactory.getLogger(HiveScan.class);

  private static int HIVE_SERDE_SCAN_OVERHEAD_FACTOR_PER_COLUMN = 20;

  private final HiveStoragePlugin hiveStoragePlugin;
  private final HiveReadEntry hiveReadEntry;
  private final HiveMetadataProvider metadataProvider;
  private final Map<String, String> confProperties;

  private List<List<LogicalInputSplit>> mappings;
  private List<LogicalInputSplit> inputSplits;

  protected List<SchemaPath> columns;

  @JsonCreator
  public HiveScan(@JsonProperty("userName") final String userName,
                  @JsonProperty("hiveReadEntry") final HiveReadEntry hiveReadEntry,
                  @JsonProperty("hiveStoragePluginConfig") final HiveStoragePluginConfig hiveStoragePluginConfig,
                  @JsonProperty("columns") final List<SchemaPath> columns,
                  @JsonProperty("confProperties") final Map<String, String> confProperties,
                  @JacksonInject final StoragePluginRegistry pluginRegistry) throws ExecutionSetupException {
    this(userName,
        hiveReadEntry,
        pluginRegistry.resolve(hiveStoragePluginConfig, HiveStoragePlugin.class),
        columns,
        null, confProperties);
  }

  public HiveScan(final String userName, final HiveReadEntry hiveReadEntry, final HiveStoragePlugin hiveStoragePlugin,
                  final List<SchemaPath> columns, final HiveMetadataProvider metadataProvider, final Map<String, String> confProperties) throws ExecutionSetupException {
    super(userName);
    this.hiveReadEntry = hiveReadEntry;
    this.columns = columns;
    this.hiveStoragePlugin = hiveStoragePlugin;
    this.confProperties = confProperties;
    if (metadataProvider == null) {
      this.metadataProvider = new HiveMetadataProvider(userName, hiveReadEntry, getHiveConf());
    } else {
      this.metadataProvider = metadataProvider;
    }
  }

  public HiveScan(final HiveScan that) {
    super(that);
    this.columns = that.columns;
    this.hiveReadEntry = that.hiveReadEntry;
    this.hiveStoragePlugin = that.hiveStoragePlugin;
    this.metadataProvider = that.metadataProvider;
    this.confProperties = that.confProperties;
  }

  public HiveScan clone(final HiveReadEntry hiveReadEntry) throws ExecutionSetupException {
    return new HiveScan(getUserName(), hiveReadEntry, hiveStoragePlugin, columns, metadataProvider, confProperties);
  }

  @JsonProperty
  public HiveReadEntry getHiveReadEntry() {
    return hiveReadEntry;
  }

  @JsonProperty
  public HiveStoragePluginConfig getHiveStoragePluginConfig() {
    return hiveStoragePlugin.getConfig();
  }

  @Override
  @JsonProperty
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonProperty
  public Map<String, String> getConfProperties() {
    return confProperties;
  }

  @JsonIgnore
  public HiveStoragePlugin getStoragePlugin() {
    return hiveStoragePlugin;
  }

  @JsonIgnore
  public HiveConf getHiveConf() {
    return HiveUtilities.generateHiveConf(hiveStoragePlugin.getHiveConf(), confProperties);
  }

  @JsonIgnore
  public boolean isNativeReader() {
    return false;
  }

  @Override
  public boolean supportsPartitionFilterPushdown() {
    List<FieldSchema> partitionKeys = hiveReadEntry.getTable().getPartitionKeys();
    return !(partitionKeys == null || partitionKeys.size() == 0);
  }

  @Override
  public void applyAssignments(final List<CoordinationProtos.DrillbitEndpoint> endpoints) {
    mappings = new ArrayList<>();
    for (int i = 0; i < endpoints.size(); i++) {
      mappings.add(new ArrayList<>());
    }
    final int count = endpoints.size();
    final List<LogicalInputSplit> inputSplits = getInputSplits();
    for (int i = 0; i < inputSplits.size(); i++) {
      mappings.get(i % count).add(inputSplits.get(i));
    }
  }

  @Override
  public SubScan getSpecificScan(final int minorFragmentId) throws ExecutionSetupException {
    try {
      final List<LogicalInputSplit> splits = mappings.get(minorFragmentId);
      List<HivePartitionWrapper> parts = new ArrayList<>();
      final List<List<String>> encodedInputSplits = new ArrayList<>();
      final List<String> splitTypes = new ArrayList<>();
      for (final LogicalInputSplit split : splits) {
        final Partition splitPartition = split.getPartition();
        if (splitPartition != null) {
          HiveTableWithColumnCache table = hiveReadEntry.getTable();
          parts.add(createPartitionWithSpecColumns(new HiveTableWithColumnCache(table, new ColumnListsCache(table)), splitPartition));
        }

        encodedInputSplits.add(split.serialize());
        splitTypes.add(split.getType());
      }
      if (parts.size() <= 0) {
        parts = null;
      }

      final HiveReadEntry subEntry = new HiveReadEntry(hiveReadEntry.getTableWrapper(), parts);
      return new HiveSubScan(getUserName(), encodedInputSplits, subEntry, splitTypes, columns, hiveStoragePlugin, confProperties);
    } catch (IOException | ReflectiveOperationException e) {
      throw new ExecutionSetupException(e);
    }
  }

  @Override
  public int getMaxParallelizationWidth() {
    return getInputSplits().size();
  }

  @Override
  public List<EndpointAffinity> getOperatorAffinity() {
    final Map<String, DrillbitEndpoint> endpointMap = new HashMap<>();
    for (final DrillbitEndpoint endpoint : hiveStoragePlugin.getContext().getBits()) {
      endpointMap.put(endpoint.getAddress(), endpoint);
      logger.debug("Endpoint address: {}", endpoint.getAddress());
    }
    final Map<DrillbitEndpoint, EndpointAffinity> affinityMap = new HashMap<>();
    try {
      long totalSize = 0;
      final List<LogicalInputSplit> inputSplits = getInputSplits();
      for (final LogicalInputSplit split : inputSplits) {
        totalSize += Math.max(1, split.getLength());
      }
      for (final LogicalInputSplit split : inputSplits) {
        final float affinity = ((float) Math.max(1, split.getLength())) / totalSize;
        for (final String loc : split.getLocations()) {
          logger.debug("Split location: {}", loc);
          final DrillbitEndpoint endpoint = endpointMap.get(loc);
          if (endpoint != null) {
            if (affinityMap.containsKey(endpoint)) {
              affinityMap.get(endpoint).addAffinity(affinity);
            } else {
              affinityMap.put(endpoint, new EndpointAffinity(endpoint, affinity));
            }
          }
        }
      }
    } catch (final IOException e) {
      throw new DrillRuntimeException(e);
    }

    return new ArrayList<>(affinityMap.values());
  }

  @Override
  public ScanStats getScanStats() {
    try {
      final HiveStats stats = metadataProvider.getStats(hiveReadEntry);

      logger.debug("HiveStats: {}", stats.toString());

      // Hive's native reader is neither memory efficient nor fast. Increase the CPU cost
      // by a factor to let the planner choose HiveDrillNativeScan over HiveScan with SerDes.
      float cpuCost = 1 * getSerDeOverheadFactor();
      return new ScanStats(GroupScanProperty.NO_EXACT_ROW_COUNT, stats.getNumRows(), cpuCost, stats.getSizeInBytes());
    } catch (final IOException e) {
      throw new DrillRuntimeException(e);
    }
  }

  @Override
  public PhysicalOperator getNewWithChildren(final List<PhysicalOperator> children) {
    return new HiveScan(this);
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @Override
  public String toString() {
    List<HivePartitionWrapper> partitions = hiveReadEntry.getHivePartitionWrappers();
    int numPartitions = partitions == null ? 0 : partitions.size();
    return "HiveScan [table=" + hiveReadEntry.getHiveTableWrapper()
        + ", columns=" + columns
        + ", numPartitions=" + numPartitions
        + ", partitions= " + partitions
        + ", inputDirectories=" + metadataProvider.getInputDirectories(hiveReadEntry)
        + ", confProperties=" + confProperties
        + "]";
  }

  @Override
  public GroupScan clone(final List<SchemaPath> columns) {
    final HiveScan newScan = new HiveScan(this);
    newScan.columns = columns;
    return newScan;
  }

  @Override
  public boolean canPushdownProjects(final List<SchemaPath> columns) {
    return true;
  }

  private List<LogicalInputSplit> getInputSplits() {
    if (inputSplits == null) {
      inputSplits = metadataProvider.getInputSplits(hiveReadEntry);
    }

    return inputSplits;
  }

  private int getSerDeOverheadFactor() {
    final int projectedColumnCount;
    if (Utilities.isStarQuery(columns)) {
      Table hiveTable = hiveReadEntry.getTable();
      projectedColumnCount = hiveTable.getSd().getColsSize() + hiveTable.getPartitionKeysSize();
    } else {
      // In cost estimation, # of project columns should be >= 1, even for skipAll query.
      projectedColumnCount = Math.max(columns.size(), 1);
    }

    return projectedColumnCount * HIVE_SERDE_SCAN_OVERHEAD_FACTOR_PER_COLUMN;
  }
}
