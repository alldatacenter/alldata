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

import static org.apache.drill.exec.planner.index.Statistics.ROWCOUNT_HUGE;
import static org.apache.drill.exec.planner.index.Statistics.ROWCOUNT_UNKNOWN;
import static org.apache.drill.exec.planner.index.Statistics.AVG_ROWSIZE_UNKNOWN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.mapr.db.impl.ConditionImpl;
import com.mapr.db.impl.ConditionNode.RowkeyRange;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.IndexGroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.ScanStats.GroupScanProperty;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.planner.index.IndexDescriptor;
import org.apache.drill.exec.planner.index.MapRDBIndexDescriptor;
import org.apache.drill.exec.planner.index.MapRDBStatisticsPayload;
import org.apache.drill.exec.planner.index.Statistics;
import org.apache.drill.exec.planner.index.MapRDBStatistics;
import org.apache.drill.exec.planner.cost.PluginCost;
import org.apache.drill.exec.planner.physical.PartitionFunction;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.dfs.FileSystemPlugin;
import org.apache.drill.exec.store.mapr.PluginConstants;
import org.apache.drill.exec.store.mapr.db.MapRDBFormatPlugin;
import org.apache.drill.exec.store.mapr.db.MapRDBFormatPluginConfig;
import org.apache.drill.exec.store.mapr.db.MapRDBGroupScan;
import org.apache.drill.exec.store.mapr.db.MapRDBSubScan;
import org.apache.drill.exec.store.mapr.db.MapRDBSubScanSpec;
import org.apache.drill.exec.store.mapr.db.MapRDBTableStats;
import org.apache.drill.exec.store.mapr.db.TabletFragmentInfo;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.exec.metastore.store.FileSystemMetadataProviderManager;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.metastore.metadata.TableMetadataProvider;
import org.ojai.store.QueryCondition;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import com.mapr.db.MetaTable;
import com.mapr.db.Table;
import com.mapr.db.impl.TabletInfoImpl;
import com.mapr.db.index.IndexDesc;
import com.mapr.db.scan.ScanRange;

@JsonTypeName("maprdb-json-scan")
public class JsonTableGroupScan extends MapRDBGroupScan implements IndexGroupScan {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JsonTableGroupScan.class);

  public static final int STAR_COLS = 100;
  public static final String TABLE_JSON = "json";
  /*
   * The <forcedRowCountMap> maintains a mapping of <RexNode, Rowcount>. These RowCounts take precedence over
   * anything computed using <MapRDBStatistics> stats. Currently, it is used for picking index plans with the
   * index_selectivity_factor. We forcibly set the full table rows as HUGE <Statistics.ROWCOUNT_HUGE> in this
   * map when the selectivity of the index is lower than index_selectivity_factor. During costing, the table
   * rowCount is returned as HUGE instead of the correct <stats> rowcount. This results in the planner choosing
   * the cheaper index plans!
   * NOTE: Full table rowCounts are specified with the NULL condition. e.g. forcedRowCountMap<NULL, 1000>
   */
  protected Map<RexNode, Double> forcedRowCountMap;
  /*
   * This stores the statistics associated with this GroupScan. Please note that the stats must be initialized
   * before using it to compute filter row counts based on query conditions.
   */
  protected MapRDBStatistics stats;
  protected JsonScanSpec scanSpec;
  protected double fullTableRowCount;
  protected double fullTableEstimatedSize;

  /**
   * need only read maxRecordsToRead records.
   */
  protected int maxRecordsToRead = -1;

  /**
   * Forced parallelization width
   */
  protected int parallelizationWidth = -1;

  @JsonCreator
  public JsonTableGroupScan(@JsonProperty("userName") final String userName,
                            @JsonProperty("scanSpec") JsonScanSpec scanSpec,
                            @JsonProperty("storage") FileSystemConfig storagePluginConfig,
                            @JsonProperty("format") MapRDBFormatPluginConfig formatPluginConfig,
                            @JsonProperty("columns") List<SchemaPath> columns,
                            @JsonProperty("schema") TupleMetadata schema,
                            @JacksonInject StoragePluginRegistry pluginRegistry) throws ExecutionSetupException {
    this(userName, pluginRegistry.resolve(storagePluginConfig, AbstractStoragePlugin.class),
        pluginRegistry.resolveFormat(storagePluginConfig, formatPluginConfig, MapRDBFormatPlugin.class),
        scanSpec, columns, new MapRDBStatistics(), FileSystemMetadataProviderManager.getMetadataProviderForSchema(schema));
  }

  public JsonTableGroupScan(String userName, AbstractStoragePlugin storagePlugin,
                            MapRDBFormatPlugin formatPlugin, JsonScanSpec scanSpec, List<SchemaPath> columns,
                            MetadataProviderManager metadataProviderManager) {
    this(userName, storagePlugin, formatPlugin, scanSpec, columns,
        new MapRDBStatistics(), FileSystemMetadataProviderManager.getMetadataProvider(metadataProviderManager));
  }

  public JsonTableGroupScan(String userName, AbstractStoragePlugin storagePlugin,
                            MapRDBFormatPlugin formatPlugin, JsonScanSpec scanSpec, List<SchemaPath> columns,
                            MapRDBStatistics stats, TableMetadataProvider metadataProvider) {
    super(storagePlugin, formatPlugin, columns, userName, metadataProvider);
    this.scanSpec = scanSpec;
    this.stats = stats;
    this.forcedRowCountMap = new HashMap<>();
    init();
  }

  /**
   * Private constructor, used for cloning.
   * @param that The HBaseGroupScan to clone
   */
  protected JsonTableGroupScan(JsonTableGroupScan that) {
    super(that);
    this.scanSpec = that.scanSpec;
    this.endpointFragmentMapping = that.endpointFragmentMapping;
    this.stats = that.stats;
    this.fullTableRowCount = that.fullTableRowCount;
    this.fullTableEstimatedSize = that.fullTableEstimatedSize;
    this.forcedRowCountMap = that.forcedRowCountMap;
    this.maxRecordsToRead = that.maxRecordsToRead;
    this.parallelizationWidth = that.parallelizationWidth;
    init();
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    JsonTableGroupScan newScan = new JsonTableGroupScan(this);
    newScan.columns = columns;
    return newScan;
  }

  public GroupScan clone(JsonScanSpec scanSpec) {
    JsonTableGroupScan newScan = new JsonTableGroupScan(this);
    newScan.scanSpec = scanSpec;
    newScan.resetRegionsToScan(); // resetting will force recalculation
    return newScan;
  }

  private void init() {
    try {
      // Get the fullTableRowCount only once i.e. if not already obtained before.
      if (fullTableRowCount == 0) {
        final Table t = this.formatPlugin.getJsonTableCache().getTable(
            scanSpec.getTableName(), scanSpec.getIndexDesc(), getUserName());
        final MetaTable metaTable = t.getMetaTable();
        // For condition null, we get full table stats.
        com.mapr.db.scan.ScanStats stats = metaTable.getScanStats();
        fullTableRowCount = stats.getEstimatedNumRows();
        fullTableEstimatedSize = stats.getEstimatedSize();
        // MapRDB client can return invalid rowCount i.e. 0, especially right after table
        // creation. It takes 15 minutes before table stats are obtained and cached in client.
        // If we get 0 rowCount, fallback to getting rowCount using old admin API.
        if (fullTableRowCount == 0) {
          PluginCost pluginCostModel = formatPlugin.getPluginCostModel();
          final int avgColumnSize = pluginCostModel.getAverageColumnSize(this);
          final int numColumns = (columns == null || columns.isEmpty() || Utilities.isStarQuery(columns)) ? STAR_COLS : columns.size();
          MapRDBTableStats tableStats = new MapRDBTableStats(formatPlugin.getFsConf(), scanSpec.getTableName());
          fullTableRowCount = tableStats.getNumRows();
          fullTableEstimatedSize = fullTableRowCount * numColumns * avgColumnSize;
        }
      }
    } catch (Exception e) {
      throw new DrillRuntimeException("Error getting region info for table: " +
        scanSpec.getTableName() + (scanSpec.getIndexDesc() == null ? "" : (", index: " + scanSpec.getIndexName())), e);
    }
  }

  @Override
  protected NavigableMap<TabletFragmentInfo, String> getRegionsToScan() {
    return getRegionsToScan(formatPlugin.getScanRangeSizeMB());
  }

  protected NavigableMap<TabletFragmentInfo, String> getRegionsToScan(int scanRangeSizeMB) {
    // If regionsToScan already computed, just return.
    double estimatedRowCount = ROWCOUNT_UNKNOWN;
    if (doNotAccessRegionsToScan == null) {
      final Table t = this.formatPlugin.getJsonTableCache().getTable(
          scanSpec.getTableName(), scanSpec.getIndexDesc(), getUserName());
      final MetaTable metaTable = t.getMetaTable();

      QueryCondition scanSpecCondition = scanSpec.getCondition();
      List<ScanRange> scanRanges = (scanSpecCondition == null)
          ? metaTable.getScanRanges(scanRangeSizeMB)
          : metaTable.getScanRanges(scanSpecCondition, scanRangeSizeMB);
      logger.debug("getRegionsToScan() with scanSpec {}: table={}, index={}, condition={}, sizeMB={}, #ScanRanges={}",
          System.identityHashCode(scanSpec), scanSpec.getTableName(), scanSpec.getIndexName(),
          scanSpec.getCondition() == null ? "null" : scanSpec.getCondition(), scanRangeSizeMB,
          scanRanges == null ? "null" : scanRanges.size());
      final TreeMap<TabletFragmentInfo, String> regionsToScan = new TreeMap<>();
      if (isIndexScan()) {
        String idxIdentifier = stats.buildUniqueIndexIdentifier(scanSpec.getIndexDesc().getPrimaryTablePath(),
            scanSpec.getIndexDesc().getIndexName());
        if (stats.isStatsAvailable()) {
          estimatedRowCount = stats.getRowCount(scanSpec.getCondition(), idxIdentifier);
        }
      } else {
        if (stats.isStatsAvailable()) {
          estimatedRowCount = stats.getRowCount(scanSpec.getCondition(), null);
        }
      }
      // If limit pushdown has occurred - factor it in the rowcount
      if (this.maxRecordsToRead > 0) {
        estimatedRowCount = Math.min(estimatedRowCount, this.maxRecordsToRead);
      }
      // If the estimated row count > 0 then scan ranges must be > 0
      Preconditions.checkState(estimatedRowCount == ROWCOUNT_UNKNOWN
          || estimatedRowCount == 0 || (scanRanges != null && scanRanges.size() > 0),
          String.format("#Scan ranges should be greater than 0 since estimated rowcount=[%f]", estimatedRowCount));
      if (scanRanges != null && scanRanges.size() > 0) {
        // set the start-row of the scanspec as the start-row of the first scan range
        ScanRange firstRange = scanRanges.get(0);
        QueryCondition firstCondition = firstRange.getCondition();
        byte[] firstStartRow = ((ConditionImpl) firstCondition).getRowkeyRanges().get(0).getStartRow();
        scanSpec.setStartRow(firstStartRow);

        // set the stop-row of ScanSpec as the stop-row of the last scan range
        ScanRange lastRange = scanRanges.get(scanRanges.size() - 1);
        QueryCondition lastCondition = lastRange.getCondition();
        List<RowkeyRange> rowkeyRanges = ((ConditionImpl) lastCondition).getRowkeyRanges();
        byte[] lastStopRow = rowkeyRanges.get(rowkeyRanges.size() - 1).getStopRow();
        scanSpec.setStopRow(lastStopRow);

        for (ScanRange range : scanRanges) {
          TabletInfoImpl tabletInfoImpl = (TabletInfoImpl) range;
          regionsToScan.put(new TabletFragmentInfo(tabletInfoImpl), range.getLocations()[0]);
        }
      }
      setRegionsToScan(regionsToScan);
    }
    return doNotAccessRegionsToScan;
  }

  @Override
  protected MapRDBSubScanSpec getSubScanSpec(final TabletFragmentInfo tfi) {
    // XXX/TODO check filter/Condition
    final JsonScanSpec spec = scanSpec;
    final String serverHostName = getRegionsToScan().get(tfi);
    QueryCondition condition = tfi.getTabletInfoImpl().getCondition();
    byte[] startRow = condition == null ? null : ((ConditionImpl) condition).getRowkeyRanges().get(0).getStartRow();
    byte[] stopRow = condition == null ? null : ((ConditionImpl) condition).getRowkeyRanges().get(0).getStopRow();
    JsonSubScanSpec subScanSpec = new JsonSubScanSpec(
        spec.getTableName(),
        spec.getIndexDesc(),
        serverHostName,
        tfi.getTabletInfoImpl().getCondition(),
        spec.getCondition(),
        startRow,
        stopRow,
        getUserName());
    return subScanSpec;
  }

  @Override
  public MapRDBSubScan getSpecificScan(int minorFragmentId) {
    assert minorFragmentId < endpointFragmentMapping.size() : String.format(
        "Mappings length [%d] should be greater than minor fragment id [%d] but it isn't.", endpointFragmentMapping.size(),
        minorFragmentId);
    return new MapRDBSubScan(getUserName(), formatPlugin, endpointFragmentMapping.get(minorFragmentId), columns, maxRecordsToRead, TABLE_JSON, getSchema());
  }

  @Override
  public ScanStats getScanStats() {
    if (isIndexScan()) {
      return indexScanStats();
    }
    return fullTableScanStats();
  }

  private ScanStats fullTableScanStats() {
    PluginCost pluginCostModel = formatPlugin.getPluginCostModel();
    final int avgColumnSize = pluginCostModel.getAverageColumnSize(this);
    final int numColumns = (columns == null || columns.isEmpty()) ? STAR_COLS : columns.size();
    // index will be NULL for FTS
    double rowCount = stats.getRowCount(scanSpec.getCondition(), null);
    // rowcount based on _id predicate. If NO _id predicate present in condition, then the
    // rowcount should be same as totalRowCount. Equality b/w the two rowcounts should not be
    // construed as NO _id predicate since stats are approximate.
    double leadingRowCount = stats.getLeadingRowCount(scanSpec.getCondition(), null);
    double avgRowSize = stats.getAvgRowSize(null, true);
    double totalRowCount = stats.getRowCount(null, null);
    logger.debug("GroupScan {} with stats {}: rowCount={}, condition={}, totalRowCount={}, fullTableRowCount={}",
            System.identityHashCode(this), System.identityHashCode(stats), rowCount,
            scanSpec.getCondition() == null ? "null" : scanSpec.getCondition(),
            totalRowCount, fullTableRowCount);
    // If UNKNOWN, or DB stats sync issues(manifests as 0 rows) use defaults.
    if (rowCount == ROWCOUNT_UNKNOWN || rowCount == 0) {
      rowCount = (scanSpec.getSerializedFilter() != null ? .5 : 1) * fullTableRowCount;
    }
    // If limit pushdown has occurred - factor it in the rowcount
    if (this.maxRecordsToRead > 0) {
      rowCount = Math.min(rowCount, this.maxRecordsToRead);
    }
    if (totalRowCount == ROWCOUNT_UNKNOWN || totalRowCount == 0) {
      logger.debug("did not get valid totalRowCount, will take this: {}", fullTableRowCount);
      totalRowCount = fullTableRowCount;
    }
    if (avgRowSize == AVG_ROWSIZE_UNKNOWN || avgRowSize == 0) {
      avgRowSize = fullTableEstimatedSize/fullTableRowCount;
    }
    double totalBlocks = getNumOfBlocks(totalRowCount, fullTableEstimatedSize, avgRowSize, pluginCostModel);
    double numBlocks = Math.min(totalBlocks, getNumOfBlocks(leadingRowCount, fullTableEstimatedSize, avgRowSize, pluginCostModel));
    double diskCost = numBlocks * pluginCostModel.getSequentialBlockReadCost(this);
    /*
     * Table scan cost made INFINITE in order to pick index plans. Use the MAX possible rowCount for
     * costing purposes.
     * NOTE: Full table rowCounts are specified with the NULL condition.
     * e.g. forcedRowCountMap<NULL, 1000>
     */
    if (forcedRowCountMap.get(null) != null && //Forced full table rowcount and it is HUGE
            forcedRowCountMap.get(null) == ROWCOUNT_HUGE ) {
      rowCount = ROWCOUNT_HUGE;
      diskCost = ROWCOUNT_HUGE;
    }

    logger.debug("JsonGroupScan:{} rowCount:{}, avgRowSize:{}, blocks:{}, totalBlocks:{}, diskCost:{}",
            this.getOperatorId(), rowCount, avgRowSize, numBlocks, totalBlocks, diskCost);
    return new ScanStats(GroupScanProperty.NO_EXACT_ROW_COUNT, rowCount, 1, diskCost);
  }

  private double getNumOfBlocks(double rowCount, double sizeFromDisk,
                                double avgRowSize, PluginCost pluginCostModel) {
    if (rowCount == ROWCOUNT_UNKNOWN || rowCount == 0) {
      return Math.ceil(sizeFromDisk / pluginCostModel.getBlockSize(this));
    } else {
      return Math.ceil(rowCount * avgRowSize / pluginCostModel.getBlockSize(this));
    }
  }

  private ScanStats indexScanStats() {
    if (!this.getIndexHint().equals("") &&
        this.getIndexHint().equals(getIndexDesc().getIndexName())) {
      logger.debug("JsonIndexGroupScan:{} forcing index {} by making tiny cost", this, this.getIndexHint());
      return new ScanStats(GroupScanProperty.NO_EXACT_ROW_COUNT, 1,1, 0);
    }

    int totalColNum = STAR_COLS;
    PluginCost pluginCostModel = formatPlugin.getPluginCostModel();
    final int avgColumnSize = pluginCostModel.getAverageColumnSize(this);
    boolean filterPushed = (scanSpec.getSerializedFilter() != null);
    if (scanSpec != null && scanSpec.getIndexDesc() != null) {
      totalColNum = scanSpec.getIndexDesc().getIncludedFields().size()
          + scanSpec.getIndexDesc().getIndexedFields().size() + 1;
    }
    int numColumns = (columns == null || columns.isEmpty()) ?  totalColNum: columns.size();
    String idxIdentifier = stats.buildUniqueIndexIdentifier(scanSpec.getIndexDesc().getPrimaryTablePath(),
        scanSpec.getIndexDesc().getIndexName());
    double rowCount = stats.getRowCount(scanSpec.getCondition(), idxIdentifier);
    // rowcount based on index leading columns predicate.
    double leadingRowCount = stats.getLeadingRowCount(scanSpec.getCondition(), idxIdentifier);
    double avgRowSize = stats.getAvgRowSize(idxIdentifier, false);
    // If UNKNOWN, use defaults
    if (rowCount == ROWCOUNT_UNKNOWN || rowCount == 0) {
      rowCount = (filterPushed ? 0.0005f : 0.001f) * fullTableRowCount / scanSpec.getIndexDesc().getIndexedFields().size();
    }
    // If limit pushdown has occurred - factor it in the rowcount
    if (this.maxRecordsToRead > 0) {
      rowCount = Math.min(rowCount, this.maxRecordsToRead);
    }
    if (leadingRowCount == ROWCOUNT_UNKNOWN || leadingRowCount == 0) {
      leadingRowCount = rowCount;
    }
    if (avgRowSize == AVG_ROWSIZE_UNKNOWN || avgRowSize == 0) {
      avgRowSize = avgColumnSize * numColumns;
    }
    double rowsFromDisk = leadingRowCount;
    if (!filterPushed) {
      // both start and stop rows are empty, indicating this is a full scan so
      // use the total rows for calculating disk i/o
      rowsFromDisk = fullTableRowCount;
    }
    double totalBlocks = Math.ceil((avgRowSize * fullTableRowCount)/pluginCostModel.getBlockSize(this));
    double numBlocks = Math.ceil(((avgRowSize * rowsFromDisk)/pluginCostModel.getBlockSize(this)));
    numBlocks = Math.min(totalBlocks, numBlocks);
    double diskCost = numBlocks * pluginCostModel.getSequentialBlockReadCost(this);
    logger.debug("index_plan_info: JsonIndexGroupScan:{} - indexName:{}: rowCount:{}, avgRowSize:{}, blocks:{}, totalBlocks:{}, rowsFromDisk {}, diskCost:{}",
        System.identityHashCode(this), scanSpec.getIndexDesc().getIndexName(), rowCount, avgRowSize, numBlocks, totalBlocks, rowsFromDisk, diskCost);
    return new ScanStats(GroupScanProperty.NO_EXACT_ROW_COUNT, rowCount, 1, diskCost);
  }

  @Override
  @JsonIgnore
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new JsonTableGroupScan(this);
  }

  @Override
  @JsonIgnore
  public String getTableName() {
    return scanSpec.getTableName();
  }

  public IndexDesc getIndexDesc() {
    return scanSpec.getIndexDesc();
  }

  public boolean isDisablePushdown() {
    return !formatPluginConfig.isEnablePushdown();
  }

  @Override
  @JsonIgnore
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return formatPluginConfig.isEnablePushdown();
  }

  @Override
  public String toString() {
    return "JsonTableGroupScan [ScanSpec=" + scanSpec + ", columns=" + columns
        + (maxRecordsToRead > 0 ? ", limit=" + maxRecordsToRead : "")
        + (getMaxParallelizationWidth() > 0 ? ", maxwidth=" + getMaxParallelizationWidth() : "") + "]";
  }

  public JsonScanSpec getScanSpec() {
    return scanSpec;
  }

  @Override
  public boolean supportsSecondaryIndex() {
    return true;
  }

  @Override
  @JsonIgnore
  public boolean isIndexScan() {
    return scanSpec != null && scanSpec.isSecondaryIndex();
  }

  @Override
  public boolean supportsRestrictedScan() {
    return true;
  }


  @Override
  public RestrictedJsonTableGroupScan getRestrictedScan(List<SchemaPath> columns) {
    RestrictedJsonTableGroupScan newScan = new RestrictedJsonTableGroupScan(this.getUserName(),
        (FileSystemPlugin) this.getStoragePlugin(),
        this.getFormatPlugin(),
        this.getScanSpec(),
        this.getColumns(),
        this.getStatistics(),
        this.getSchema());

    newScan.columns = columns;
    return newScan;
  }

  /**
   * Get the estimated average rowsize. DO NOT call this API directly.
   * Call the stats API instead which modifies the counts based on preference options.
   * @param index to use for generating the estimate
   * @return row count post filtering
   */
  public MapRDBStatisticsPayload getAverageRowSizeStats(IndexDescriptor index) {
    IndexDesc indexDesc = null;
    double avgRowSize = AVG_ROWSIZE_UNKNOWN;

    if (index != null) {
      indexDesc = (IndexDesc)((MapRDBIndexDescriptor)index).getOriginalDesc();
    }
    // If no index is specified, get it from the primary table
    if (indexDesc == null && scanSpec.isSecondaryIndex()) {
      throw new UnsupportedOperationException("getAverageRowSizeStats should be invoked on primary table");
    }

    // Get the index table or primary table and use the DB API to get the estimated number of rows. For size estimates,
    // we assume that all the columns would be read from the disk.
    final Table table = this.formatPlugin.getJsonTableCache().getTable(scanSpec.getTableName(), indexDesc, getUserName());

    if (table != null) {
      final MetaTable metaTable = table.getMetaTable();
      if (metaTable != null) {
        avgRowSize = metaTable.getAverageRowSize();
      }
    }
    logger.debug("index_plan_info: getEstimatedRowCount obtained from DB Client for {}: indexName: {}, indexInfo: {}, " +
            "avgRowSize: {}, estimatedSize {}", this, (indexDesc == null ? "null" : indexDesc.getIndexName()),
        (indexDesc == null ? "null" : indexDesc.getIndexInfo()), avgRowSize, fullTableEstimatedSize);
    return new MapRDBStatisticsPayload(ROWCOUNT_UNKNOWN, ROWCOUNT_UNKNOWN, avgRowSize);
  }

  /**
   * Get the estimated statistics after applying the {@link RexNode} condition. DO NOT call this API directly.
   * Call the stats API instead which modifies the counts based on preference options.
   * @param condition filter to apply
   * @param index to use for generating the estimate
   * @param scanRel the current scan rel
   * @return row count post filtering
   */
  public MapRDBStatisticsPayload getFirstKeyEstimatedStats(QueryCondition condition, IndexDescriptor index, RelNode scanRel) {
    IndexDesc indexDesc = null;
    if (index != null) {
      indexDesc = (IndexDesc)((MapRDBIndexDescriptor)index).getOriginalDesc();
    }
    return getFirstKeyEstimatedStatsInternal(condition, indexDesc, scanRel);
  }

  /**
   * Get the estimated statistics after applying the {@link QueryCondition} condition
   * @param condition filter to apply
   * @param index to use for generating the estimate
   * @param scanRel the current scan rel
   * @return {@link MapRDBStatisticsPayload} statistics
   */
  private MapRDBStatisticsPayload getFirstKeyEstimatedStatsInternal(QueryCondition condition, IndexDesc index, RelNode scanRel) {

    // If no index is specified, get it from the primary table
    if (index == null && scanSpec.isSecondaryIndex()) {
      // If stats not cached get it from the table.
      // table = MapRDB.getTable(scanSpec.getPrimaryTablePath());
      throw new UnsupportedOperationException("getFirstKeyEstimatedStats should be invoked on primary table");
    }

    // Get the index table or primary table and use the DB API to get the estimated number of rows. For size estimates,
    // we assume that all the columns would be read from the disk.
    final Table table = this.formatPlugin.getJsonTableCache().getTable(scanSpec.getTableName(), index, getUserName());

    if (table != null) {
      // Factor reflecting confidence in the DB estimates. If a table has few tablets, the tablet-level stats
      // might be off. The decay scalingFactor will reduce estimates when one tablet represents a significant percentage
      // of the entire table.
      double scalingFactor = 1.0;
      boolean isFullScan = false;
      final MetaTable metaTable = table.getMetaTable();
      com.mapr.db.scan.ScanStats stats = (condition == null)
          ? metaTable.getScanStats() : metaTable.getScanStats(condition);
      if (index == null && condition != null) {
        // Given table condition might not be on leading column. Check if the rowcount matches full table rows.
        // In that case no leading key present or does not prune enough. Treat it like so.
        com.mapr.db.scan.ScanStats noConditionPTabStats = metaTable.getScanStats();
        if (stats.getEstimatedNumRows() == noConditionPTabStats.getEstimatedNumRows()) {
          isFullScan = true;
        }
      }
      // Use the scalingFactor only when a condition filters out rows from the table. If no condition is present, all rows
      // should be selected. So the scalingFactor should not reduce the returned rows
      if (condition != null && !isFullScan) {
        double forcedScalingFactor = PrelUtil.getSettings(scanRel.getCluster()).getIndexStatsRowCountScalingFactor();
        // Accuracy is defined as 1 - Error where Error = # Boundary Tablets (2) / # Total Matching Tablets.
        // For 2 or less matching tablets, the error is assumed to be 50%. The Sqrt gives the decaying scalingFactor
        if (stats.getTabletCount() > 2) {
          double accuracy = 1.0 - (2.0/stats.getTabletCount());
          scalingFactor = Math.min(1.0, 1.0 / Math.sqrt(1.0 / accuracy));
        } else {
          scalingFactor = 0.5;
        }
        if (forcedScalingFactor < 1.0
            && metaTable.getScanStats().getTabletCount() < PluginConstants.JSON_TABLE_NUM_TABLETS_PER_INDEX_DEFAULT) {
          // User forced confidence scalingFactor for small tables (assumed as less than 32 tablets (~512 MB))
          scalingFactor = forcedScalingFactor;
        }
      }
      logger.info("index_plan_info: getEstimatedRowCount obtained from DB Client for {}: indexName: {}, indexInfo: {}, " +
              "condition: {} rowCount: {}, avgRowSize: {}, estimatedSize {}, tabletCount {}, totalTabletCount {}, " +
              "scalingFactor {}",
          this, (index == null ? "null" : index.getIndexName()), (index == null ? "null" : index.getIndexInfo()),
          (condition == null ? "null" : condition.toString()), stats.getEstimatedNumRows(),
          (stats.getEstimatedNumRows() == 0 ? 0 : stats.getEstimatedSize()/stats.getEstimatedNumRows()),
          stats.getEstimatedSize(), stats.getTabletCount(), metaTable.getScanStats().getTabletCount(), scalingFactor);
      return new MapRDBStatisticsPayload(scalingFactor * stats.getEstimatedNumRows(), scalingFactor * stats.getEstimatedNumRows(),
          ((stats.getEstimatedNumRows() == 0 ? 0 : (double)stats.getEstimatedSize()/stats.getEstimatedNumRows())));
    } else {
      logger.info("index_plan_info: getEstimatedRowCount: {} indexName: {}, indexInfo: {}, " +
              "condition: {} rowCount: UNKNOWN, avgRowSize: UNKNOWN", this, (index == null ? "null" : index.getIndexName()),
          (index == null ? "null" : index.getIndexInfo()), (condition == null ? "null" : condition.toString()));
      return new MapRDBStatisticsPayload(ROWCOUNT_UNKNOWN, ROWCOUNT_UNKNOWN, AVG_ROWSIZE_UNKNOWN);
    }
  }


  /**
   * Set the row count resulting from applying the {@link RexNode} condition. Forced row counts will take
   * precedence over stats row counts
   * @param condition filter to apply
   * @param count row count
   * @param capRowCount row count limit
   */
  @Override
  @JsonIgnore
  public void setRowCount(RexNode condition, double count, double capRowCount) {
    forcedRowCountMap.put(condition, count);
  }

  @Override
  public void setStatistics(Statistics statistics) {
    assert statistics instanceof MapRDBStatistics : String.format(
        "Passed unexpected statistics instance. Expects MAPR-DB Statistics instance");
    this.stats = ((MapRDBStatistics) statistics);
  }

  /**
   * Get the row count after applying the {@link RexNode} condition
   * @param condition filter to apply
   * @param scanRel the current scan rel
   * @return row count post filtering
   */
  @Override
  @JsonIgnore
  public double getRowCount(RexNode condition, RelNode scanRel) {
    // Do not use statistics if row count is forced. Forced rowcounts take precedence over stats
    double rowcount;
    if (forcedRowCountMap.get(condition) != null) {
      return forcedRowCountMap.get(condition);
    }
    if (scanSpec.getIndexDesc() != null) {
      String idxIdentifier = stats.buildUniqueIndexIdentifier(scanSpec.getIndexDesc().getPrimaryTablePath(),
          scanSpec.getIndexName());
      rowcount = stats.getRowCount(condition, idxIdentifier, scanRel);
    } else {
      rowcount = stats.getRowCount(condition, null, scanRel);
    }
    // Stats might NOT have the full rows (e.g. table is newly populated and DB stats APIs return it after
    // 15 mins). Use the table rows as populated using the (expensive but accurate) Hbase API if needed.
    if (condition == null && (rowcount == 0 || rowcount == ROWCOUNT_UNKNOWN)) {
      rowcount = fullTableRowCount;
      logger.debug("getRowCount: Stats not available yet! Use Admin APIs full table rowcount {}",
          fullTableRowCount);
    }
    return rowcount;
  }

  @Override
  public boolean isDistributed() {
    // getMaxParallelizationWidth gets information about all regions to scan and is expensive.
    // This option is meant to be used only for unit tests.
    boolean useNumRegions = storagePlugin.getContext().getConfig().getBoolean(PluginConstants.JSON_TABLE_USE_NUM_REGIONS_FOR_DISTRIBUTION_PLANNING);
    double fullTableSize;

    if (useNumRegions) {
      return getMaxParallelizationWidth() > 1;
    }

    // This function gets called multiple times during planning. To avoid performance
    // bottleneck, estimate degree of parallelization using stats instead of actually getting information
    // about all regions.
    double rowCount, rowSize;
    double scanRangeSize = storagePlugin.getContext().getConfig().getInt(PluginConstants.JSON_TABLE_SCAN_SIZE_MB) * 1024 * 1024;

    if (scanSpec.getIndexDesc() != null) {
      String idxIdentifier = stats.buildUniqueIndexIdentifier(scanSpec.getIndexDesc().getPrimaryTablePath(), scanSpec.getIndexName());
      rowCount = stats.getRowCount(scanSpec.getCondition(), idxIdentifier);
      rowSize = stats.getAvgRowSize(idxIdentifier, false);
    } else {
      rowCount = stats.getRowCount(scanSpec.getCondition(), null);
      rowSize = stats.getAvgRowSize(null, false);
    }

    if (rowCount == ROWCOUNT_UNKNOWN || rowCount == 0 ||
        rowSize == AVG_ROWSIZE_UNKNOWN || rowSize == 0) {
      fullTableSize = (scanSpec.getSerializedFilter() != null ? .5 : 1) * this.fullTableEstimatedSize;
    } else {
      fullTableSize = rowCount * rowSize;
    }

    return (long) fullTableSize / scanRangeSize > 1;
  }

  @Override
  public MapRDBStatistics getStatistics() {
    return stats;
  }

  @Override
  @JsonIgnore
  public void setColumns(List<SchemaPath> columns) {
    this.columns = columns;
  }

  @Override
  @JsonIgnore
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @Override
  @JsonIgnore
  public PartitionFunction getRangePartitionFunction(List<FieldReference> refList) {
    return new JsonTableRangePartitionFunction(refList, scanSpec.getTableName(), this.getUserName(), this.getFormatPlugin());
  }

  /**
   * Convert a given {@link LogicalExpression} condition into a {@link QueryCondition} condition
   * @param condition expressed as a {@link LogicalExpression}
   * @return {@link QueryCondition} condition equivalent to the given expression
   */
  @JsonIgnore
  public QueryCondition convertToQueryCondition(LogicalExpression condition) {
    final JsonConditionBuilder jsonConditionBuilder = new JsonConditionBuilder(this, condition);
    final JsonScanSpec newScanSpec = jsonConditionBuilder.parseTree();
    if (newScanSpec != null) {
      return newScanSpec.getCondition();
    } else {
      return null;
    }
  }

  /**
   * Checks if Json table reader supports limit push down.
   *
   * @return true if limit push down is supported, false otherwise
   */
  @Override
  public boolean supportsLimitPushdown() {
    if (maxRecordsToRead < 0) {
      return true;
    }
    return false; // limit is already pushed. No more pushdown of limit
  }

  @Override
  public GroupScan applyLimit(int maxRecords) {
    maxRecordsToRead = Math.max(maxRecords, 1);
    return this;
  }

  @Override
  public int getMaxParallelizationWidth() {
    if (this.parallelizationWidth > 0) {
      return this.parallelizationWidth;
    }
    return super.getMaxParallelizationWidth();
  }

  @Override
  public void setParallelizationWidth(int width) {
    if (width > 0) {
      this.parallelizationWidth = width;
      logger.debug("Forced parallelization width = {}", width);
    }
  }
}
