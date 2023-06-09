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
package org.apache.drill.exec.store.mapr.db.binary;

import static org.apache.drill.exec.store.mapr.db.util.CommonFns.isNullOrEmpty;

import java.util.List;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.ScanStats.GroupScanProperty;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.planner.index.Statistics;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.hbase.DrillHBaseConstants;
import org.apache.drill.exec.store.hbase.HBaseScanSpec;
import org.apache.drill.exec.store.hbase.HBaseUtils;
import org.apache.drill.exec.store.mapr.db.MapRDBFormatPlugin;
import org.apache.drill.exec.store.mapr.db.MapRDBFormatPluginConfig;
import org.apache.drill.exec.store.mapr.db.MapRDBGroupScan;
import org.apache.drill.exec.store.mapr.db.MapRDBSubScan;
import org.apache.drill.exec.store.mapr.db.MapRDBSubScanSpec;
import org.apache.drill.exec.store.mapr.db.MapRDBTableStats;
import org.apache.drill.exec.store.mapr.db.TabletFragmentInfo;
import org.apache.drill.exec.metastore.store.FileSystemMetadataProviderManager;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.metastore.metadata.TableMetadataProvider;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.RegionLocator;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeName("maprdb-binary-scan")
public class BinaryTableGroupScan extends MapRDBGroupScan implements DrillHBaseConstants {
  private static final Logger logger = LoggerFactory.getLogger(BinaryTableGroupScan.class);

  public static final String TABLE_BINARY = "binary";

  private final HBaseScanSpec hbaseScanSpec;

  private HTableDescriptor hTableDesc;

  private MapRDBTableStats tableStats;

  @JsonCreator
  public BinaryTableGroupScan(@JsonProperty("userName") final String userName,
                              @JsonProperty("hbaseScanSpec") HBaseScanSpec scanSpec,
                              @JsonProperty("storage") FileSystemConfig storagePluginConfig,
                              @JsonProperty("format") MapRDBFormatPluginConfig formatPluginConfig,
                              @JsonProperty("columns") List<SchemaPath> columns,
                              @JsonProperty("schema") TupleMetadata schema,
                              @JacksonInject StoragePluginRegistry pluginRegistry) throws ExecutionSetupException {
    this(userName, pluginRegistry.resolve(storagePluginConfig, AbstractStoragePlugin.class),
        pluginRegistry.resolveFormat(storagePluginConfig, formatPluginConfig, MapRDBFormatPlugin.class),
        scanSpec, columns, null /* tableStats */, FileSystemMetadataProviderManager.getMetadataProviderForSchema(schema));
  }

  public BinaryTableGroupScan(String userName, AbstractStoragePlugin storagePlugin,
      MapRDBFormatPlugin formatPlugin, HBaseScanSpec scanSpec, List<SchemaPath> columns,
      MetadataProviderManager metadataProviderManager) {
    this(userName, storagePlugin, formatPlugin, scanSpec,
        columns, null /* tableStats */, FileSystemMetadataProviderManager.getMetadataProvider(metadataProviderManager));
  }

  public BinaryTableGroupScan(String userName, AbstractStoragePlugin storagePlugin,
      MapRDBFormatPlugin formatPlugin, HBaseScanSpec scanSpec,
      List<SchemaPath> columns, MapRDBTableStats tableStats, TableMetadataProvider metadataProvider) {
    super(storagePlugin, formatPlugin, columns, userName, metadataProvider);
    this.hbaseScanSpec = scanSpec;
    this.tableStats = tableStats;
    init();
  }

  /**
   * Private constructor, used for cloning.
   * @param that The HBaseGroupScan to clone
   */
  private BinaryTableGroupScan(BinaryTableGroupScan that) {
    super(that);
    this.hbaseScanSpec = that.hbaseScanSpec;
    this.endpointFragmentMapping = that.endpointFragmentMapping;
    this.hTableDesc = that.hTableDesc;
    this.tableStats = that.tableStats;
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    BinaryTableGroupScan newScan = new BinaryTableGroupScan(this);
    newScan.columns = columns == null ? ALL_COLUMNS : columns;
    HBaseUtils.verifyColumns(columns, hTableDesc);
    return newScan;
  }

  private void init() {
    logger.debug("Getting region locations");
    TableName tableName = TableName.valueOf(hbaseScanSpec.getTableName());
    try (Admin admin = formatPlugin.getConnection().getAdmin();
         RegionLocator locator = formatPlugin.getConnection().getRegionLocator(tableName)) {
      hTableDesc = admin.getTableDescriptor(tableName);
      // Fetch tableStats only once and cache it.
      if (tableStats == null) {
        tableStats = new MapRDBTableStats(getHBaseConf(), hbaseScanSpec.getTableName());
      }
      boolean foundStartRegion = false;
      final TreeMap<TabletFragmentInfo, String> regionsToScan = new TreeMap<>();
      List<HRegionLocation> regionLocations = locator.getAllRegionLocations();
      for (HRegionLocation regionLocation : regionLocations) {
        HRegionInfo regionInfo = regionLocation.getRegionInfo();
        if (!foundStartRegion && hbaseScanSpec.getStartRow() != null && hbaseScanSpec.getStartRow().length != 0 && !regionInfo.containsRow(hbaseScanSpec.getStartRow())) {
          continue;
        }
        foundStartRegion = true;
        regionsToScan.put(new TabletFragmentInfo(regionInfo), regionLocation.getHostname());
        if (hbaseScanSpec.getStopRow() != null && hbaseScanSpec.getStopRow().length != 0 && regionInfo.containsRow(hbaseScanSpec.getStopRow())) {
          break;
        }
      }
      setRegionsToScan(regionsToScan);
    } catch (Exception e) {
      throw new DrillRuntimeException("Error getting region info for table: " + hbaseScanSpec.getTableName(), e);
    }
    HBaseUtils.verifyColumns(columns, hTableDesc);
  }


  @Override
  protected MapRDBSubScanSpec getSubScanSpec(TabletFragmentInfo tfi) {
    HBaseScanSpec spec = hbaseScanSpec;
    return new MapRDBSubScanSpec(
        spec.getTableName(),
        null /* indexFid */,
        getRegionsToScan().get(tfi),
        (!isNullOrEmpty(spec.getStartRow()) && tfi.containsRow(spec.getStartRow())) ? spec.getStartRow() : tfi.getStartKey(),
        (!isNullOrEmpty(spec.getStopRow()) && tfi.containsRow(spec.getStopRow())) ? spec.getStopRow() : tfi.getEndKey(),
        spec.getSerializedFilter(),
        null,
        getUserName());
  }

  @Override
  public MapRDBSubScan getSpecificScan(int minorFragmentId) {
    assert minorFragmentId < endpointFragmentMapping.size() : String.format(
        "Mappings length [%d] should be greater than minor fragment id [%d] but it isn't.", endpointFragmentMapping.size(),
        minorFragmentId);
    return new MapRDBSubScan(getUserName(), formatPlugin, endpointFragmentMapping.get(minorFragmentId), columns, TABLE_BINARY, getSchema());
  }

  @Override
  public ScanStats getScanStats() {
    //TODO: look at stats for this.
    long rowCount = (long) ((hbaseScanSpec.getFilter() != null ? .5 : 1) * tableStats.getNumRows());
    int avgColumnSize = 10;
    int numColumns = (columns == null || columns.isEmpty()) ? 100 : columns.size();
    return new ScanStats(GroupScanProperty.NO_EXACT_ROW_COUNT, rowCount, 1, avgColumnSize * numColumns * rowCount);
  }

  @Override
  @JsonIgnore
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new BinaryTableGroupScan(this);
  }

  @JsonIgnore
  public Configuration getHBaseConf() {
    return getFormatPlugin().getHBaseConf();
  }

  @Override
  @JsonIgnore
  public String getTableName() {
    return getHBaseScanSpec().getTableName();
  }

  @JsonIgnore
  public MapRDBTableStats getTableStats() {
    return tableStats;
  }

  @Override
  public String toString() {
    return "BinaryTableGroupScan [ScanSpec="
        + hbaseScanSpec + ", columns="
        + columns + "]";
  }

  @JsonProperty
  public HBaseScanSpec getHBaseScanSpec() {
    return hbaseScanSpec;
  }

  @Override
  public void setRowCount(RexNode condition, double count, double capRowCount) {
    throw new UnsupportedOperationException("setRowCount() not implemented for BinaryTableGroupScan");
  }

  @Override
  public double getRowCount(RexNode condition, RelNode scanRel) {
    return Statistics.ROWCOUNT_UNKNOWN;
  }

  @Override
  public Statistics getStatistics() {
    throw new UnsupportedOperationException("getStatistics() not implemented for BinaryTableGroupScan");
  }

  @Override
  @JsonIgnore
  public boolean isIndexScan() {
    return false;
  }

}
