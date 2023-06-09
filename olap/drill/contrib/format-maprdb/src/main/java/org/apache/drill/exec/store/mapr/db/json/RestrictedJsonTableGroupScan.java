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
package org.apache.drill.exec.store.mapr.db.json;

import java.util.List;
import java.util.NavigableMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.metastore.store.FileSystemMetadataProviderManager;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.ScanStats.GroupScanProperty;
import org.apache.drill.exec.planner.index.MapRDBStatistics;
import org.apache.drill.exec.planner.cost.PluginCost;
import org.apache.drill.exec.planner.index.Statistics;
import org.apache.drill.exec.store.dfs.FileSystemPlugin;
import org.apache.drill.exec.store.mapr.db.MapRDBFormatPlugin;
import org.apache.drill.exec.store.mapr.db.MapRDBSubScan;
import org.apache.drill.exec.store.mapr.db.MapRDBSubScanSpec;
import org.apache.drill.exec.store.mapr.db.RestrictedMapRDBSubScan;
import org.apache.drill.exec.store.mapr.db.RestrictedMapRDBSubScanSpec;
import org.apache.drill.exec.store.mapr.db.TabletFragmentInfo;

/**
 * A RestrictedJsonTableGroupScan encapsulates (along with a subscan) the functionality
 * for doing restricted (i.e skip) scan rather than sequential scan.  The skipping is based
 * on a supplied set of row keys (primary keys) from a join operator.
 */
@JsonTypeName("restricted-json-scan")
public class RestrictedJsonTableGroupScan extends JsonTableGroupScan {

  @JsonCreator
  public RestrictedJsonTableGroupScan(@JsonProperty("userName") String userName,
                                      @JsonProperty("storage") FileSystemPlugin storagePlugin,
                                      @JsonProperty("format") MapRDBFormatPlugin formatPlugin,
                                      @JsonProperty("scanSpec") JsonScanSpec scanSpec, /* scan spec of the original table */
                                      @JsonProperty("columns") List<SchemaPath> columns,
                                      @JsonProperty("") MapRDBStatistics statistics,
                                      @JsonProperty("schema") TupleMetadata schema) {
    super(userName, storagePlugin, formatPlugin, scanSpec, columns,
        statistics, FileSystemMetadataProviderManager.getMetadataProviderForSchema(schema));
  }

  // TODO:  this method needs to be fully implemented
  protected RestrictedMapRDBSubScanSpec getSubScanSpec(TabletFragmentInfo tfi) {
    JsonScanSpec spec = scanSpec;
    RestrictedMapRDBSubScanSpec subScanSpec =
        new RestrictedMapRDBSubScanSpec(
        spec.getTableName(),
        getRegionsToScan().get(tfi), spec.getSerializedFilter(), getUserName());
    return subScanSpec;
  }

  protected NavigableMap<TabletFragmentInfo, String> getRegionsToScan() {
    return getRegionsToScan(formatPlugin.getRestrictedScanRangeSizeMB());
  }

  @Override
  public MapRDBSubScan getSpecificScan(int minorFragmentId) {
    assert minorFragmentId < endpointFragmentMapping.size() : String.format(
        "Mappings length [%d] should be greater than minor fragment id [%d] but it isn't.", endpointFragmentMapping.size(),
        minorFragmentId);
    RestrictedMapRDBSubScan subscan =
        new RestrictedMapRDBSubScan(getUserName(), formatPlugin,
        getEndPointFragmentMapping(minorFragmentId), columns, maxRecordsToRead, TABLE_JSON, getSchema());

    return subscan;
  }

  private List<RestrictedMapRDBSubScanSpec> getEndPointFragmentMapping(int minorFragmentId) {
    List<RestrictedMapRDBSubScanSpec> restrictedSubScanSpecList = Lists.newArrayList();
    List<MapRDBSubScanSpec> subScanSpecList = endpointFragmentMapping.get(minorFragmentId);
    for (MapRDBSubScanSpec s : subScanSpecList) {
      restrictedSubScanSpecList.add((RestrictedMapRDBSubScanSpec) s);
    }
    return restrictedSubScanSpecList;
  }

  /**
   * Private constructor, used for cloning.
   * @param that The RestrictedJsonTableGroupScan to clone
   */
  private RestrictedJsonTableGroupScan(RestrictedJsonTableGroupScan that) {
    super(that);
  }

  @Override
  public GroupScan clone(JsonScanSpec scanSpec) {
    RestrictedJsonTableGroupScan newScan = new RestrictedJsonTableGroupScan(this);
    newScan.scanSpec = scanSpec;
    newScan.resetRegionsToScan(); // resetting will force recalculation
    return newScan;
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    RestrictedJsonTableGroupScan newScan = new RestrictedJsonTableGroupScan(this);
    newScan.columns = columns;
    return newScan;
  }

  @Override
  @JsonIgnore
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new RestrictedJsonTableGroupScan(this);
  }

  @Override
  public ScanStats getScanStats() {
    // TODO: ideally here we should use the rowcount from index scan, and multiply a factor of restricted scan
    double rowCount;
    PluginCost pluginCostModel = formatPlugin.getPluginCostModel();
    final int avgColumnSize = pluginCostModel.getAverageColumnSize(this);
    int numColumns = (columns == null || columns.isEmpty()) ?  STAR_COLS: columns.size();
    // Get the restricted group scan row count - same as the right side index rows
    rowCount = computeRestrictedScanRowcount();
    // Get the average row size of the primary table
    double avgRowSize = stats.getAvgRowSize(null, true);
    if (avgRowSize == Statistics.AVG_ROWSIZE_UNKNOWN || avgRowSize == 0) {
      avgRowSize = avgColumnSize * numColumns;
    }
    // restricted scan does random lookups and each row may belong to a different block, with the number
    // of blocks upper bounded by the total num blocks in the primary table
    double totalBlocksPrimary = Math.ceil((avgRowSize * fullTableRowCount)/pluginCostModel.getBlockSize(this));
    double numBlocks = Math.min(totalBlocksPrimary, rowCount);
    double diskCost = numBlocks * pluginCostModel.getRandomBlockReadCost(this);
    // For non-covering plans, the dominating cost would be of the join back. Reduce it using the factor
    // for biasing towards non-covering plans.
    diskCost *= stats.getRowKeyJoinBackIOFactor();
    logger.debug("RestrictedJsonGroupScan:{} rowCount:{}, avgRowSize:{}, blocks:{}, totalBlocks:{}, diskCost:{}",
        System.identityHashCode(this), rowCount, avgRowSize, numBlocks, totalBlocksPrimary, diskCost);
    return new ScanStats(GroupScanProperty.NO_EXACT_ROW_COUNT, rowCount, 1, diskCost);
  }

  private double computeRestrictedScanRowcount() {
    double rowCount = Statistics.ROWCOUNT_UNKNOWN;
    // The rowcount should be the same as the build side which was FORCED by putting it in forcedRowCountMap
    if (forcedRowCountMap.get(null) != null) {
      rowCount = forcedRowCountMap.get(null);
    }
    // If limit pushdown has occurred - factor it in the rowcount
    if (rowCount == Statistics.ROWCOUNT_UNKNOWN || rowCount == 0) {
      rowCount = (0.001f * fullTableRowCount);
    }
    if (this.maxRecordsToRead > 0) {
      rowCount = Math.min(rowCount, this.maxRecordsToRead);
    }
    return rowCount;
  }

  @Override
  public boolean isRestrictedScan() {
    return true;
  }

  @Override
  public String toString() {
    return "RestrictedJsonTableGroupScan [ScanSpec=" + scanSpec + ", columns=" + columns
        + ", rowcount=" + computeRestrictedScanRowcount()
        + (maxRecordsToRead > 0 ? ", limit=" + maxRecordsToRead : "")
        + (getMaxParallelizationWidth() > 0 ? ", maxwidth=" + getMaxParallelizationWidth() : "") + "]";
  }
}
