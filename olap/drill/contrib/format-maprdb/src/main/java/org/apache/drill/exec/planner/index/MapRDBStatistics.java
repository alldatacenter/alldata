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
package org.apache.drill.exec.planner.index;

import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMdUtil;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.Pair;
import org.apache.drill.common.expression.ExpressionStringBuilder;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.logical.DrillOptiq;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.store.hbase.HBaseRegexParser;
import org.apache.drill.exec.store.mapr.db.json.JsonTableGroupScan;
import org.apache.hadoop.hbase.HConstants;
import org.ojai.store.QueryCondition;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public class MapRDBStatistics implements Statistics {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MapRDBStatistics.class);
  static final String nullConditionAsString = "<NULL>";
  private double rowKeyJoinBackIOFactor = 1.0;
  private boolean statsAvailable = false;
  private StatisticsPayload fullTableScanPayload = null;
  /*
   * The computed statistics are cached in <statsCache> so that any subsequent calls are returned
   * from the cache. The <statsCache> is a map of <RexNode, map<Index, Stats Payload>>. The <RexNode>
   * does not have a comparator so it is converted to a String for serving as a Map key. This may result
   * in logically equivalent conditions considered differently e.g. sal<10 OR sal>100, sal>100 OR sal<10
   * the second map maintains statistics per index as not all statistics are independent of the index
   * e.g. average row size.
   */
  private Map<String, Map<String, StatisticsPayload>> statsCache;
  /*
   * The filter independent computed statistics are cached in <fIStatsCache> so that any subsequent
   * calls are returned from the cache. The <fIStatsCache> is a map of <Index, Stats Payload>. This
   * cache maintains statistics per index as not all statistics are independent of the index
   * e.g. average row size.
   */
  private Map<String, StatisticsPayload> fIStatsCache;
  /*
  /*
   * The mapping between <QueryCondition> and <RexNode> is kept in <conditionRexNodeMap>. This mapping
   * is useful to obtain rowCount for condition specified as <QueryCondition> required during physical
   * planning. Again, both the <QueryCondition> and <RexNode> are converted to Strings for the lack
   * of a comparator.
   */
  private Map<String, String> conditionRexNodeMap;

  public MapRDBStatistics() {
    statsCache = new HashMap<>();
    fIStatsCache = new HashMap<>();
    conditionRexNodeMap = new HashMap<>();
  }

  public double getRowKeyJoinBackIOFactor() {
    return rowKeyJoinBackIOFactor;
  }

  @Override
  public boolean isStatsAvailable() {
    return statsAvailable;
  }

  @Override
  public String buildUniqueIndexIdentifier(IndexDescriptor idx) {
    if (idx == null) {
      return null;
    } else {
      return idx.getTableName() + "_" + idx.getIndexName();
    }
  }

  public String buildUniqueIndexIdentifier(String tableName, String idxName) {
    if (tableName == null || idxName == null) {
      return null;
    } else {
      return tableName + "_" + idxName;
    }
  }

  @Override
  /** Returns the number of rows satisfying the given FILTER condition
   *  @param condition - FILTER specified as a {@link RexNode}
   *  @param tabIdxName - The table/index identifier
   *  @return approximate rows satisfying the filter
   */
  public double getRowCount(RexNode condition, String tabIdxName, RelNode scanRel) {
    String conditionAsStr = nullConditionAsString;
    Map<String, StatisticsPayload> payloadMap;
    if ((scanRel instanceof DrillScanRel && ((DrillScanRel)scanRel).getGroupScan() instanceof DbGroupScan)
        || (scanRel instanceof ScanPrel && ((ScanPrel)scanRel).getGroupScan() instanceof DbGroupScan)) {
      if (condition == null && fullTableScanPayload != null) {
        return fullTableScanPayload.getRowCount();
      } else if (condition != null) {
        conditionAsStr = convertRexToString(condition, scanRel.getRowType());
        payloadMap = statsCache.get(conditionAsStr);
        if (payloadMap != null) {
          if (payloadMap.get(tabIdxName) != null) {
            return payloadMap.get(tabIdxName).getRowCount();
          } else {
            // We might not have computed rowcount for the given condition from the tab/index in question.
            // For rowcount it does not matter which index was used to get the rowcount for the given condition.
            // Hence, just use the first one!
            for (String payloadKey : payloadMap.keySet()) {
              if (payloadKey != null && payloadMap.get(payloadKey) != null) {
                return payloadMap.get(payloadKey).getRowCount();
              }
            }
            StatisticsPayload anyPayload = payloadMap.entrySet().iterator().next().getValue();
            return anyPayload.getRowCount();
          }
        }
      }
    }
    if (statsAvailable) {
      logger.debug("Statistics: Filter row count is UNKNOWN for filter: {}", conditionAsStr);
    }
    return ROWCOUNT_UNKNOWN;
  }

  /** Returns the number of rows satisfying the given FILTER condition
   *  @param condition - FILTER specified as a {@link QueryCondition}
   *  @param tabIdxName - The table/index identifier
   *  @return approximate rows satisfying the filter
   */
  public double getRowCount(QueryCondition condition, String tabIdxName) {
    String conditionAsStr = nullConditionAsString;
    Map<String, StatisticsPayload> payloadMap;
    if (condition != null
        && conditionRexNodeMap.get(condition.toString()) != null) {
      String rexConditionAsString = conditionRexNodeMap.get(condition.toString());
      payloadMap = statsCache.get(rexConditionAsString);
      if (payloadMap != null) {
        if (payloadMap.get(tabIdxName) != null) {
          return payloadMap.get(tabIdxName).getRowCount();
        } else {
          // We might not have computed rowcount for the given condition from the tab/index in question.
          // For rowcount it does not matter which index was used to get the rowcount for the given condition.
          // if tabIdxName is null, most likely we have found one from payloadMap and won't come to here.
          // If we come to here, we are looking for payload for an index, so let us use any index's payload first!
          for (String payloadKey : payloadMap.keySet()) {
            if (payloadKey != null && payloadMap.get(payloadKey) != null) {
              return payloadMap.get(payloadKey).getRowCount();
            }
          }
          StatisticsPayload anyPayload = payloadMap.entrySet().iterator().next().getValue();
          return anyPayload.getRowCount();
        }
      }
    } else if (condition == null
        && fullTableScanPayload != null) {
      return fullTableScanPayload.getRowCount();
    }
    if (condition != null) {
      conditionAsStr = condition.toString();
    }
    if (statsAvailable) {
      logger.debug("Statistics: Filter row count is UNKNOWN for filter: {}", conditionAsStr);
    }
    return ROWCOUNT_UNKNOWN;
  }

  /** Returns the number of leading rows satisfying the given FILTER condition
   *  @param condition - FILTER specified as a {@link RexNode}
   *  @param tabIdxName - The table/index identifier
   *  @param scanRel - The current scanRel
   *  @return approximate rows satisfying the leading filter
   */
  @Override
  public double getLeadingRowCount(RexNode condition, String tabIdxName, DrillScanRelBase scanRel) {
    String conditionAsStr = nullConditionAsString;
    Map<String, StatisticsPayload> payloadMap;
    if ((scanRel instanceof DrillScanRel && ((DrillScanRel)scanRel).getGroupScan() instanceof DbGroupScan)
        || (scanRel instanceof ScanPrel && ((ScanPrel)scanRel).getGroupScan() instanceof DbGroupScan)) {
      if (condition == null && fullTableScanPayload != null) {
        return fullTableScanPayload.getLeadingRowCount();
      } else if (condition != null) {
        conditionAsStr = convertRexToString(condition, scanRel.getRowType());
        payloadMap = statsCache.get(conditionAsStr);
        if (payloadMap != null) {
          if (payloadMap.get(tabIdxName) != null) {
            return payloadMap.get(tabIdxName).getLeadingRowCount();
          }
          // Unlike rowcount, leading rowcount is dependent on the index. So, if tab/idx is
          // not found, we are out of luck!
        }
      }
    }
    if (statsAvailable) {
      logger.debug("Statistics: Leading filter row count is UNKNOWN for filter: {}", conditionAsStr);
    }
    return ROWCOUNT_UNKNOWN;
  }

  /** Returns the number of leading rows satisfying the given FILTER condition
   *  @param condition - FILTER specified as a {@link QueryCondition}
   *  @param tabIdxName - The table/index identifier
   *  @return approximate rows satisfying the leading filter
   */
  public double getLeadingRowCount(QueryCondition condition, String tabIdxName) {
    String conditionAsStr = nullConditionAsString;
    Map<String, StatisticsPayload> payloadMap;
    if (condition != null
        && conditionRexNodeMap.get(condition.toString()) != null) {
      String rexConditionAsString = conditionRexNodeMap.get(condition.toString());
      payloadMap = statsCache.get(rexConditionAsString);
      if (payloadMap != null) {
        if (payloadMap.get(tabIdxName) != null) {
          return payloadMap.get(tabIdxName).getLeadingRowCount();
        }
        // Unlike rowcount, leading rowcount is dependent on the index. So, if tab/idx is
        // not found, we are out of luck!
      }
    } else if (condition == null
        && fullTableScanPayload != null) {
      return fullTableScanPayload.getLeadingRowCount();
    }
    if (condition != null) {
      conditionAsStr = condition.toString();
    }
    if (statsAvailable) {
      logger.debug("Statistics: Leading filter row count is UNKNOWN for filter: {}", conditionAsStr);
    }
    return ROWCOUNT_UNKNOWN;
  }

  @Override
  public double getAvgRowSize(String tabIdxName, boolean isTableScan) {
    StatisticsPayload payloadMap;
    if (isTableScan && fullTableScanPayload != null) {
      return fullTableScanPayload.getAvgRowSize();
    } else if (!isTableScan) {
      payloadMap = fIStatsCache.get(tabIdxName);
      if (payloadMap != null) {
        return payloadMap.getAvgRowSize();
      }
    }
    if (statsAvailable) {
      logger.debug("Statistics: Average row size is UNKNOWN for table: {}", tabIdxName);
    }
    return AVG_ROWSIZE_UNKNOWN;
  }

  public boolean initialize(RexNode condition, DrillScanRelBase scanRel, IndexCallContext context) {
    GroupScan scan = IndexPlanUtils.getGroupScan(scanRel);

    PlannerSettings settings = PrelUtil.getPlannerSettings(scanRel.getCluster().getPlanner());
    rowKeyJoinBackIOFactor = settings.getIndexRowKeyJoinCostFactor();
    if (scan instanceof DbGroupScan) {
      String conditionAsStr = convertRexToString(condition, scanRel.getRowType());
      if (statsCache.get(conditionAsStr) == null) {
        IndexCollection indexes = ((DbGroupScan)scan).getSecondaryIndexCollection(scanRel);
        populateStats(condition, indexes, scanRel, context);
        logger.info("index_plan_info: initialize: scanRel #{} and groupScan {} got fulltable {}, statsCache: {}, fiStatsCache: {}",
            scanRel.getId(), System.identityHashCode(scan), fullTableScanPayload, statsCache, fIStatsCache);
        return true;
      }
    }
    return false;
  }

  /**
   * This function computes statistics when there is no query condition
   * @param jTabGrpScan - The current group scan
   * @param indexes - The collection of indexes to use for getting statistics
   * @param scanRel - The current scanRel
   * @param context - The index plan call context
   */
  private void populateStatsForNoFilter(JsonTableGroupScan jTabGrpScan, IndexCollection indexes, RelNode scanRel,
                                   IndexCallContext context) {
    // Get the stats payload for full table (has total rows in the table)
    StatisticsPayload ftsPayload = jTabGrpScan.getFirstKeyEstimatedStats(null, null, scanRel);
    addToCache(null, null, context, ftsPayload, jTabGrpScan, scanRel, scanRel.getRowType());
    addToCache(null, jTabGrpScan.getAverageRowSizeStats(null), ftsPayload);
    // Get the stats for all indexes
    for (IndexDescriptor idx: indexes) {
      StatisticsPayload idxPayload = jTabGrpScan.getFirstKeyEstimatedStats(null, idx, scanRel);
      StatisticsPayload idxRowSizePayload = jTabGrpScan.getAverageRowSizeStats(idx);
      RelDataType newRowType;
      FunctionalIndexInfo functionInfo = idx.getFunctionalInfo();
      if (functionInfo.hasFunctional()) {
        newRowType = FunctionalIndexHelper.rewriteFunctionalRowType(scanRel, context, functionInfo);
      } else {
        newRowType = scanRel.getRowType();
      }
      addToCache(null, idx, context, idxPayload, jTabGrpScan, scanRel, newRowType);
      addToCache(idx, idxRowSizePayload, ftsPayload);
    }
  }

  /**
   * This is the core statistics function for populating the statistics. The statistics populated correspond to the query
   * condition. Based on different types of plans, we would need statistics for different combinations of predicates. Currently,
   * we do not have a tree-walker for {@link QueryCondition}. Hence, instead of using the individual predicates stats, to construct
   * the stats for the overall predicates, we rely on using the final predicates. Hence, this has a limitation(susceptible) to
   * predicate modification post stats generation. Statistics computed/stored are rowcounts, leading rowcounts, average rowsize.
   * Rowcounts and leading rowcounts (i.e. corresponding to predicates on the leading index columns) are stored in the statsCache.
   * Average rowsizes are stored in the fiStatsCache (FI stands for Filter Independent).
   *
   * @param condition - The condition for which to obtain statistics
   * @param indexes - The collection of indexes to use for getting statistics
   * @param scanRel - The current scanRel
   * @param context - The index plan call context
   */
  private void populateStats(RexNode condition, IndexCollection indexes, DrillScanRelBase scanRel,
                               IndexCallContext context) {
    JsonTableGroupScan jTabGrpScan;
    Map<IndexDescriptor, IndexConditionInfo> firstKeyIdxConditionMap;
    Map<IndexDescriptor, IndexConditionInfo> idxConditionMap;
    /* Map containing the individual base conditions of an ANDed/ORed condition and their selectivities.
     * This is used to compute the overall selectivity of a complex ANDed/ORed condition using its base
     * conditions. Helps prevent over/under estimates and guessed selectivity for ORed predicates.
     */
    Map<String, Double> baseConditionMap;
    GroupScan grpScan = IndexPlanUtils.getGroupScan(scanRel);

    if ((scanRel instanceof DrillScanRel || scanRel instanceof ScanPrel) &&
        grpScan instanceof JsonTableGroupScan) {
      jTabGrpScan = (JsonTableGroupScan) grpScan;
    } else {
      logger.debug("Statistics: populateStats exit early - not an instance of JsonTableGroupScan!");
      return;
    }
    if (condition == null) {
      populateStatsForNoFilter(jTabGrpScan, indexes, scanRel, context);
      statsAvailable = true;
      return;
    }

    RexBuilder builder = scanRel.getCluster().getRexBuilder();
    PlannerSettings settings = PrelUtil.getSettings(scanRel.getCluster());
    // Get the stats payload for full table (has total rows in the table)
    StatisticsPayload ftsPayload = jTabGrpScan.getFirstKeyEstimatedStats(null, null, scanRel);

    // Get the average row size for table and all indexes
    addToCache(null, jTabGrpScan.getAverageRowSizeStats(null), ftsPayload);
    if (ftsPayload == null || ftsPayload.getRowCount() == 0) {
      return;
    }
    for (IndexDescriptor idx : indexes) {
      StatisticsPayload idxRowSizePayload = jTabGrpScan.getAverageRowSizeStats(idx);
      addToCache(idx, idxRowSizePayload, ftsPayload);
    }

    /* Only use indexes with distinct first key */
    IndexCollection distFKeyIndexes = distinctFKeyIndexes(indexes, scanRel);
    IndexConditionInfo.Builder infoBuilder = IndexConditionInfo.newBuilder(condition,
        distFKeyIndexes, builder, scanRel);
    idxConditionMap = infoBuilder.getIndexConditionMap();
    firstKeyIdxConditionMap = infoBuilder.getFirstKeyIndexConditionMap();
    baseConditionMap = new HashMap<>();
    for (IndexDescriptor idx : firstKeyIdxConditionMap.keySet()) {
      if(IndexPlanUtils.conditionIndexed(context.getOrigMarker(), idx) == IndexPlanUtils.ConditionIndexed.NONE) {
        continue;
      }
      RexNode idxCondition = firstKeyIdxConditionMap.get(idx).indexCondition;
      /* Use the pre-processed condition only for getting actual statistic from MapR-DB APIs. Use the
       * original condition everywhere else (cache store/lookups) since the RexNode condition and its
       * corresponding QueryCondition will be used to get statistics. e.g. we convert LIKE into RANGE
       * condition to get statistics. However, statistics are always asked for LIKE and NOT the RANGE
       */
      RexNode preProcIdxCondition = convertToStatsCondition(idxCondition, idx, context, scanRel,
          Arrays.asList(SqlKind.CAST, SqlKind.LIKE));
      RelDataType newRowType;
      FunctionalIndexInfo functionInfo = idx.getFunctionalInfo();
      if (functionInfo.hasFunctional()) {
        newRowType = FunctionalIndexHelper.rewriteFunctionalRowType(scanRel, context, functionInfo);
      } else {
        newRowType = scanRel.getRowType();
      }

      QueryCondition queryCondition = jTabGrpScan.convertToQueryCondition(
          convertToLogicalExpression(preProcIdxCondition, newRowType, settings, builder));
      // Cap rows/size at total rows in case of issues with DB APIs
      StatisticsPayload idxPayload = jTabGrpScan.getFirstKeyEstimatedStats(queryCondition, idx, scanRel);
      double rowCount = Math.min(idxPayload.getRowCount(), ftsPayload.getRowCount());
      double leadingRowCount = Math.min(idxPayload.getLeadingRowCount(), rowCount);
      double avgRowSize = Math.min(idxPayload.getAvgRowSize(), ftsPayload.getAvgRowSize());
      StatisticsPayload payload = new MapRDBStatisticsPayload(rowCount, leadingRowCount, avgRowSize);
      addToCache(idxCondition, idx, context, payload, jTabGrpScan, scanRel, newRowType);
      addBaseConditions(idxCondition, payload, false, baseConditionMap, scanRel.getRowType());
    }
    /* Add the row count for index conditions on all indexes. Stats are only computed for leading
     * keys but index conditions can be pushed and would be required for access path costing
     */
    for (IndexDescriptor idx : idxConditionMap.keySet()) {
      if(IndexPlanUtils.conditionIndexed(context.getOrigMarker(), idx) == IndexPlanUtils.ConditionIndexed.NONE) {
        continue;
      }
      Map<LogicalExpression, RexNode> leadingPrefixMap = Maps.newHashMap();
      double rowCount, leadingRowCount, avgRowSize;
      RexNode idxCondition = idxConditionMap.get(idx).indexCondition;
      // Ignore conditions which always evaluate to true
      if (idxCondition.isAlwaysTrue()) {
        continue;
      }
      RexNode idxIncColCondition = idxConditionMap.get(idx).remainderCondition;
      RexNode idxRemColCondition = IndexPlanUtils.getLeadingPrefixMap(leadingPrefixMap, idx.getIndexColumns(), infoBuilder, idxCondition);
      RexNode idxLeadColCondition = IndexPlanUtils.getLeadingColumnsFilter(
          IndexPlanUtils.getLeadingFilters(leadingPrefixMap, idx.getIndexColumns()), builder);
      RexNode idxTotRemColCondition = IndexPlanUtils.getTotalRemainderFilter(idxRemColCondition, idxIncColCondition, builder);
      RexNode idxTotColCondition = IndexPlanUtils.getTotalFilter(idxLeadColCondition, idxTotRemColCondition, builder);
      FunctionalIndexInfo functionInfo = idx.getFunctionalInfo();
      RelDataType newRowType = scanRel.getRowType();
      if (functionInfo.hasFunctional()) {
        newRowType = FunctionalIndexHelper.rewriteFunctionalRowType(scanRel, context, functionInfo);
      }
      /* For non-covering plans we would need the index leading condition */
      rowCount = ftsPayload.getRowCount() * computeSelectivity(idxLeadColCondition, idx,
          ftsPayload.getRowCount(), scanRel, baseConditionMap).left;
      leadingRowCount = rowCount;
      avgRowSize = fIStatsCache.get(buildUniqueIndexIdentifier(idx)).getAvgRowSize();
      addToCache(idxLeadColCondition, idx, context, new MapRDBStatisticsPayload(rowCount, leadingRowCount, avgRowSize),
          jTabGrpScan, scanRel, newRowType);
      /* For covering plans we would need the full condition */
      rowCount = ftsPayload.getRowCount() * computeSelectivity(idxTotColCondition, idx,
          ftsPayload.getRowCount(), scanRel, baseConditionMap).left;
      addToCache(idxTotColCondition, idx, context, new MapRDBStatisticsPayload(rowCount, leadingRowCount, avgRowSize),
          jTabGrpScan, scanRel, newRowType);
      /* For intersect plans we would need the index condition */
      rowCount = ftsPayload.getRowCount() * computeSelectivity(idxCondition, idx,
          ftsPayload.getRowCount(), scanRel, baseConditionMap).left;
      addToCache(idxCondition, idx, context, new MapRDBStatisticsPayload(rowCount, leadingRowCount, avgRowSize),
          jTabGrpScan, scanRel, newRowType);
      /* Add the rowCount for condition on only included columns - no leading columns here! */
      if (idxIncColCondition != null) {
        rowCount = ftsPayload.getRowCount() * computeSelectivity(idxIncColCondition, null,
            ftsPayload.getRowCount(), scanRel, baseConditionMap).left;
        addToCache(idxIncColCondition, idx, context, new MapRDBStatisticsPayload(rowCount, rowCount, avgRowSize),
            jTabGrpScan, scanRel, newRowType);
      }
    }

    // Add the rowCount for the complete condition - based on table
    double rowCount = ftsPayload.getRowCount() * computeSelectivity(condition, null,
        ftsPayload.getRowCount(), scanRel, baseConditionMap).left;
    // Here, ftsLeadingKey rowcount is based on _id predicates
    StatisticsPayload ftsLeadingKeyPayload = jTabGrpScan.getFirstKeyEstimatedStats(jTabGrpScan.convertToQueryCondition(
        convertToLogicalExpression(condition, scanRel.getRowType(), settings, builder)), null, scanRel);
    addToCache(condition, null, null, new MapRDBStatisticsPayload(rowCount, ftsLeadingKeyPayload.getRowCount(),
        ftsPayload.getAvgRowSize()), jTabGrpScan, scanRel, scanRel.getRowType());
    // Add the full table rows while we are at it - represented by <NULL> RexNode, <NULL> QueryCondition.
    // No ftsLeadingKey so leadingKeyRowcount = totalRowCount
    addToCache(null, null, null, new MapRDBStatisticsPayload(ftsPayload.getRowCount(), ftsPayload.getRowCount(),
        ftsPayload.getAvgRowSize()), jTabGrpScan, scanRel, scanRel.getRowType());
    // mark stats has been statsAvailable
    statsAvailable = true;
  }

  private boolean addBaseConditions(RexNode condition, StatisticsPayload payload, boolean redundant,
      Map<String, Double> baseConditionMap, RelDataType rowType) {
    boolean res = redundant;
    if (condition.getKind() == SqlKind.AND) {
      for(RexNode pred : RelOptUtil.conjunctions(condition)) {
        res = addBaseConditions(pred, payload, res, baseConditionMap, rowType);
      }
    } else if (condition.getKind() == SqlKind.OR) {
      for(RexNode pred : RelOptUtil.disjunctions(condition)) {
        res = addBaseConditions(pred, payload, res, baseConditionMap, rowType);
      }
    } else {
      // base condition
      String conditionAsStr = convertRexToString(condition, rowType);
      if (!redundant) {
        baseConditionMap.put(conditionAsStr, payload.getRowCount());
        return true;
      } else {
        baseConditionMap.put(conditionAsStr, -1.0);
        return false;
      }
    }
    return res;
  }
  /*
   * Adds the statistic(row count) to the cache. Also adds the corresponding QueryCondition->RexNode
   * condition mapping.
   */
  private void addToCache(RexNode condition, IndexDescriptor idx, IndexCallContext context,
      StatisticsPayload payload, JsonTableGroupScan jTabGrpScan, RelNode scanRel, RelDataType rowType) {
    if (condition != null
        && !condition.isAlwaysTrue()) {
      RexBuilder builder = scanRel.getCluster().getRexBuilder();
      PlannerSettings settings = PrelUtil.getSettings(scanRel.getCluster());
      String conditionAsStr = convertRexToString(condition, scanRel.getRowType());
      if (statsCache.get(conditionAsStr) == null
              && payload.getRowCount() != Statistics.ROWCOUNT_UNKNOWN) {
        Map<String, StatisticsPayload> payloadMap = new HashMap<>();
        payloadMap.put(buildUniqueIndexIdentifier(idx), payload);
        statsCache.put(conditionAsStr, payloadMap);
        logger.debug("Statistics: StatsCache:<{}, {}>",conditionAsStr, payload);
        // Always pre-process CAST conditions - Otherwise queryCondition will not be generated correctly
        RexNode preProcIdxCondition = convertToStatsCondition(condition, idx, context, scanRel,
            Arrays.asList(SqlKind.CAST));
        QueryCondition queryCondition =
            jTabGrpScan.convertToQueryCondition(convertToLogicalExpression(preProcIdxCondition,
                rowType, settings, builder));
        if (queryCondition != null) {
          String queryConditionAsStr = queryCondition.toString();
          if (conditionRexNodeMap.get(queryConditionAsStr) == null) {
            conditionRexNodeMap.put(queryConditionAsStr, conditionAsStr);
            logger.debug("Statistics: QCRNCache:<{}, {}>",queryConditionAsStr, conditionAsStr);
          }
        } else {
          logger.debug("Statistics: QCRNCache: Unable to generate QueryCondition for {}", conditionAsStr);
          logger.debug("Statistics: QCRNCache: Unable to generate QueryCondition for {}", conditionAsStr);
        }
      } else {
        Map<String, StatisticsPayload> payloadMap = statsCache.get(conditionAsStr);
        if (payloadMap != null) {
          if (payloadMap.get(buildUniqueIndexIdentifier(idx)) == null) {
            payloadMap.put(buildUniqueIndexIdentifier(idx), payload);

            // rowCount for the same condition should be the same on primary table or index,
            // let us sync them to the smallest since currently both are over-estimated.
            // DO NOT sync the leading rowCount since it is based on the leading condition and not the
            // condition (key for this cache). Hence, for the same condition the leading condition and
            // consequently the leading rowCount will vary with the index. Syncing them may lead to
            // unintended side-effects e.g. given a covering index and full table scan and a condition
            // on a non-id field which happens to be the leading key in the index, the leading rowcount
            // for the full table scan should be the full table rowcount. Syncing them would incorrectly
            // make the full table scan cheaper! If required, syncing should be only done based on
            // leading condition and NOT the condition
            double minimalRowCount = payload.getRowCount();
            for (StatisticsPayload existing : payloadMap.values()) {
              if (existing.getRowCount() < minimalRowCount) {
                minimalRowCount = existing.getRowCount();
              }
            }
            for (StatisticsPayload existing : payloadMap.values()) {
              if (existing instanceof MapRDBStatisticsPayload) {
                ((MapRDBStatisticsPayload)existing).rowCount = minimalRowCount;
              }
            }
          } else {
            logger.debug("Statistics: Filter row count already exists for filter: {}. Skip!", conditionAsStr);
          }
        } else {
          logger.debug("Statistics: Filter row count is UNKNOWN for filter: {}", conditionAsStr);
        }
      }
    } else if (condition == null && idx == null) {
      fullTableScanPayload = new MapRDBStatisticsPayload(payload.getRowCount(),
          payload.getLeadingRowCount(), payload.getAvgRowSize());
      logger.debug("Statistics: StatsCache:<{}, {}>","NULL", fullTableScanPayload);
    }
  }

  private void addToCache(IndexDescriptor idx, StatisticsPayload payload, StatisticsPayload ftsPayload) {
    String tabIdxIdentifier = buildUniqueIndexIdentifier(idx);
    if (fIStatsCache.get(tabIdxIdentifier) == null) {
      if (ftsPayload.getAvgRowSize() >= payload.getAvgRowSize()) {
        fIStatsCache.put(tabIdxIdentifier, payload);
        logger.debug("Statistics: fIStatsCache:<{}, {}>",tabIdxIdentifier, payload);
      } else {
        StatisticsPayload cappedPayload =
            new MapRDBStatisticsPayload(ROWCOUNT_UNKNOWN, ROWCOUNT_UNKNOWN, ftsPayload.getAvgRowSize());
        fIStatsCache.put(tabIdxIdentifier,cappedPayload);
        logger.debug("Statistics: fIStatsCache:<{}, {}> (Capped)",tabIdxIdentifier, cappedPayload);
      }
    } else {
      logger.debug("Statistics: Average row size already exists for :<{}, {}>. Skip!",tabIdxIdentifier, payload);
    }
  }

  /*
   * Convert the given RexNode to a String representation while also replacing the RexInputRef references
   * to actual column names. Since, we compare String representations of RexNodes, two equivalent RexNode
   * expressions may differ in the RexInputRef positions but otherwise the same.
   * e.g. $1 = 'CA' projection (State, Country) , $2 = 'CA' projection (Country, State)
   */
  private String convertRexToString(RexNode condition, RelDataType rowType) {
    StringBuilder sb = new StringBuilder();
    if (condition == null) {
      return null;
    }
    if (condition.getKind() == SqlKind.AND) {
      boolean first = true;
      for(RexNode pred : RelOptUtil.conjunctions(condition)) {
        if (first) {
          sb.append(convertRexToString(pred, rowType));
          first = false;
        } else {
          sb.append(" " + SqlKind.AND.toString() + " ");
          sb.append(convertRexToString(pred, rowType));
        }
      }
      return sb.toString();
    } else if (condition.getKind() == SqlKind.OR) {
      boolean first = true;
      for(RexNode pred : RelOptUtil.disjunctions(condition)) {
        if (first) {
          sb.append(convertRexToString(pred, rowType));
          first = false;
        } else {
          sb.append(" " + SqlKind.OR.toString() + " ");
          sb.append(convertRexToString(pred, rowType));
        }
      }
      return sb.toString();
    } else {
      HashMap<String, String> inputRefMapping = new HashMap<>();
      /* Based on the rel projection the input reference for the same column may change
       * during planning. We want the cache to be agnostic to it. Hence, the entry stored
       * in the cache has the input reference ($i) replaced with the column name
       */
      getInputRefMapping(condition, rowType, inputRefMapping);
      if (inputRefMapping.keySet().size() > 0) {
        //Found input ref - replace it
        String replCondition = condition.toString();
        for (String inputRef : inputRefMapping.keySet()) {
          replCondition = replCondition.replace(inputRef, inputRefMapping.get(inputRef));
        }
        return replCondition;
      } else {
        return condition.toString();
      }
    }
  }

  /*
   * Generate the input reference to column mapping for reference replacement. Please
   * look at the usage in convertRexToString() to understand why this mapping is required.
   */
  private void getInputRefMapping(RexNode condition, RelDataType rowType,
      HashMap<String, String> mapping) {
    if (condition instanceof RexCall) {
      for (RexNode op : ((RexCall) condition).getOperands()) {
        getInputRefMapping(op, rowType, mapping);
      }
    } else if (condition instanceof RexInputRef) {
      mapping.put(condition.toString(),
          rowType.getFieldNames().get(condition.hashCode()));
    }
  }

  /*
   * Additional pre-processing may be required for LIKE/CAST predicates in order to compute statistics.
   * e.g. A LIKE predicate should be converted to a RANGE predicate for statistics computation. MapR-DB
   * does not yet support computing statistics for LIKE predicates.
   */
  private RexNode convertToStatsCondition(RexNode condition, IndexDescriptor index,
      IndexCallContext context, RelNode scanRel, List<SqlKind>typesToProcess) {
    RexBuilder builder = scanRel.getCluster().getRexBuilder();
    if (condition.getKind() == SqlKind.AND) {
      final List<RexNode> conditions = Lists.newArrayList();
      for(RexNode pred : RelOptUtil.conjunctions(condition)) {
        conditions.add(convertToStatsCondition(pred, index, context, scanRel, typesToProcess));
      }
      return RexUtil.composeConjunction(builder, conditions, false);
    } else if (condition.getKind() == SqlKind.OR) {
      final List<RexNode> conditions = Lists.newArrayList();
      for(RexNode pred : RelOptUtil.disjunctions(condition)) {
        conditions.add(convertToStatsCondition(pred, index, context, scanRel, typesToProcess));
      }
      return RexUtil.composeDisjunction(builder, conditions, false);
    } else if (condition instanceof RexCall) {
      // LIKE operator - convert to a RANGE predicate, if possible
      if (typesToProcess.contains(SqlKind.LIKE)
          && ((RexCall) condition).getOperator().getKind() == SqlKind.LIKE) {
        return convertLikeToRange((RexCall)condition, builder);
      } else if (typesToProcess.contains(SqlKind.CAST)
          && hasCastExpression(condition)) {
        return convertCastForFIdx(((RexCall) condition), index, context, scanRel);
      }
      else {
        return condition;
      }
    }
    return condition;
  }

  /*
   * Determines whether the given expression contains a CAST expression. Assumes that the
   * given expression is a valid expression.
   * Returns TRUE, if it finds at least one instance of CAST operator.
   */
  private boolean hasCastExpression(RexNode condition) {
    if (condition instanceof RexCall) {
      if (((RexCall) condition).getOperator().getKind() == SqlKind.CAST) {
        return true;
      }
      for (RexNode op : ((RexCall) condition).getOperands()) {
        if (hasCastExpression(op)) {
          return true;
        }
      }
    }
    return false;
  }
  /*
   * CAST expressions are not understood by MAPR-DB as-is. Hence, we must convert them before passing them
   * onto MAPR-DB for statistics. Given a functional index, the given expression is converted into an
   * expression on the `expression` column of the functional index.
   */
  private RexNode convertCastForFIdx(RexCall condition, IndexDescriptor index,
                                     IndexCallContext context, RelNode origScan) {
    if (index == null) {
      return condition;
    }
    FunctionalIndexInfo functionInfo = index.getFunctionalInfo();
    if (!functionInfo.hasFunctional()) {
      return condition;
    }
    // The functional index has a different row-type than the original scan. Use the index row-type when
    // converting the condition
    RelDataType newRowType = FunctionalIndexHelper.rewriteFunctionalRowType(origScan, context, functionInfo);
    RexBuilder builder = origScan.getCluster().getRexBuilder();
    return FunctionalIndexHelper.convertConditionForIndexScan(condition,
        origScan, newRowType, builder, functionInfo);
  }

  /*
   * Helper function to perform additional pre-processing for LIKE predicates
   */
  private RexNode convertLikeToRange(RexCall condition, RexBuilder builder) {
    Preconditions.checkArgument(condition.getOperator().getKind() == SqlKind.LIKE,
        "Unable to convertLikeToRange: argument is not a LIKE condition!");
    HBaseRegexParser parser = null;
    RexNode arg = null;
    RexLiteral pattern = null, escape = null;
    String patternStr = null, escapeStr = null;
    if (condition.getOperands().size() == 2) {
      // No escape character specified
      for (RexNode op : condition.getOperands()) {
        if (op.getKind() == SqlKind.LITERAL) {
          pattern = (RexLiteral) op;
        } else {
          arg = op;
        }
      }
      // Get the PATTERN strings from the corresponding RexLiteral
      if (pattern.getTypeName() == SqlTypeName.DECIMAL ||
          pattern.getTypeName() == SqlTypeName.INTEGER) {
        patternStr = pattern.getValue().toString();
      } else if (pattern.getTypeName() == SqlTypeName.CHAR) {
        patternStr = pattern.getValue2().toString();
      }
      if (patternStr != null) {
        parser = new HBaseRegexParser(patternStr);
      }
    } else if (condition.getOperands().size() == 3) {
      // Escape character specified
      for (RexNode op : condition.getOperands()) {
        if (op.getKind() == SqlKind.LITERAL) {
          // Assume first literal specifies PATTERN and the second literal specifies the ESCAPE char
          if (pattern == null) {
            pattern = (RexLiteral) op;
          } else {
            escape = (RexLiteral) op;
          }
        } else {
          arg = op;
        }
      }
      // Get the PATTERN and ESCAPE strings from the corresponding RexLiteral
      if (pattern.getTypeName() == SqlTypeName.DECIMAL ||
          pattern.getTypeName() == SqlTypeName.INTEGER) {
        patternStr = pattern.getValue().toString();
      } else if (pattern.getTypeName() == SqlTypeName.CHAR) {
        patternStr = pattern.getValue2().toString();
      }
      if (escape.getTypeName() == SqlTypeName.DECIMAL ||
          escape.getTypeName() == SqlTypeName.INTEGER) {
        escapeStr = escape.getValue().toString();
      } else if (escape.getTypeName() == SqlTypeName.CHAR) {
        escapeStr = escape.getValue2().toString();
      }
      if (patternStr != null && escapeStr != null) {
        parser = new HBaseRegexParser(patternStr, escapeStr.toCharArray()[0]);
      }
    }
    if (parser != null) {
      parser.parse();
      String prefix = parser.getPrefixString();
      /*
       * If there is a literal prefix, convert it into an EQUALITY or RANGE predicate
       */
      if (prefix != null) {
        if (prefix.equals(parser.getLikeString())) {
          // No WILDCARD present. This turns the LIKE predicate to EQUALITY predicate
          if (arg != null) {
            return builder.makeCall(SqlStdOperatorTable.EQUALS, arg, pattern);
          }
        } else {
          // WILDCARD present. This turns the LIKE predicate to RANGE predicate
          byte[] startKey = HConstants.EMPTY_START_ROW;
          byte[] stopKey = HConstants.EMPTY_END_ROW;
          startKey = prefix.getBytes(Charsets.UTF_8);
          stopKey = startKey.clone();
          boolean isMaxVal = true;
          for (int i = stopKey.length - 1; i >= 0; --i) {
            int nextByteValue = (0xff & stopKey[i]) + 1;
            if (nextByteValue < 0xff) {
              stopKey[i] = (byte) nextByteValue;
              isMaxVal = false;
              break;
            } else {
              stopKey[i] = 0;
            }
          }
          if (isMaxVal) {
            stopKey = HConstants.EMPTY_END_ROW;
          }
          try {
            // TODO: This maybe a potential bug since we assume UTF-8 encoding. However, we follow the
            // current DB implementation. See HBaseFilterBuilder.createHBaseScanSpec "like" CASE statement
            RexLiteral startKeyLiteral = builder.makeLiteral(new String(startKey,
                Charsets.UTF_8.toString()));
            RexLiteral stopKeyLiteral = builder.makeLiteral(new String(stopKey,
                Charsets.UTF_8.toString()));
            if (arg != null) {
              RexNode startPred = builder.makeCall(SqlStdOperatorTable.GREATER_THAN_OR_EQUAL,
                  arg, startKeyLiteral);
              RexNode stopPred = builder.makeCall(SqlStdOperatorTable.LESS_THAN, arg, stopKeyLiteral);
              return builder.makeCall(SqlStdOperatorTable.AND, startPred, stopPred);
            }
          } catch (UnsupportedEncodingException ex) {
            // Encoding not supported - Do nothing!
            logger.debug("Statistics: convertLikeToRange: Unsupported Encoding Exception -> {}", ex.getMessage());
          }
        }
      }
    }
    // Could not convert - return condition as-is.
    return condition;
  }

  /*
   * Compute the selectivity of the given rowCondition. Retrieve the selectivity
   * for index conditions from the cache
   */
  private Pair<Double, Boolean> computeSelectivity(RexNode condition, IndexDescriptor idx, double totalRows,
      RelNode scanRel, Map<String, Double> baseConditionMap) {
    double selectivity;
    boolean guess = false;
    if (totalRows <= 0) {
      return new Pair<>(1.0, true);
    }
    String conditionAsStr = convertRexToString(condition, scanRel.getRowType());
    if (condition.getKind() == SqlKind.AND) {
      selectivity = 1.0;
      for (RexNode pred : RelOptUtil.conjunctions(condition)) {
        Pair<Double, Boolean> selPayload = computeSelectivity(pred, idx, totalRows, scanRel, baseConditionMap);
        if (selPayload.left > 0) {
          // At least one AND branch is a guess
          if (selPayload.right == true) {
            guess = true;
          }
          selectivity *= selPayload.left;
        }
      }
    } else if (condition.getKind() == SqlKind.OR) {
      selectivity = 0.0;
      for (RexNode pred : RelOptUtil.disjunctions(condition)) {
        Pair<Double, Boolean> selPayload = computeSelectivity(pred, idx, totalRows, scanRel, baseConditionMap);
        if (selPayload.left > 0.0) {
          // At least one OR branch is a guess
          if (selPayload.right == true) {
            guess = true;
          }
          selectivity += selPayload.left;
        }
      }
      // Cap selectivity of OR'ed predicates at 0.25 if at least one predicate is a guess (Calcite does the same)
      if (guess && selectivity > 0.25) {
        selectivity = 0.25;
      }
    } else {
      guess = false;
      if (baseConditionMap.get(conditionAsStr) != null) {
        double rowCount = baseConditionMap.get(conditionAsStr);
        if (rowCount != -1.0) {
          selectivity = rowCount / totalRows;
        } else {
          // Ignore
          selectivity = -1.0;
          guess = true;
        }
      } else {
        selectivity = RelMdUtil.guessSelectivity(condition);
        guess = true;
      }
      return new Pair<>(selectivity, guess);
    }
    // Cap selectivity to be between 0.0 and 1.0
    selectivity = Math.min(1.0, selectivity);
    selectivity = Math.max(0.0, selectivity);
    logger.debug("Statistics: computeSelectivity: Cache MISS: Computed {} -> {}", conditionAsStr, selectivity);
    return new Pair<>(selectivity, guess);
  }

  /*
   * Filters out indexes from the given collection based on the row key of indexes i.e. after filtering
   * the given collection would contain only one index for each distinct row key in the collection
   */
  private IndexCollection distinctFKeyIndexes(IndexCollection indexes, RelNode scanRel) {
    IndexCollection distinctIdxCollection = new DrillIndexCollection(scanRel, new HashSet<DrillIndexDescriptor>());
    Iterator<IndexDescriptor> iterator = indexes.iterator();
    Map<String, List<IndexDescriptor>> firstColIndexMap = new HashMap<>();
    while (iterator.hasNext()) {
      IndexDescriptor index = iterator.next();
      // If index has columns - the first column is the leading column for the index
      if (index.getIndexColumns() != null) {
        List<IndexDescriptor> idxList;
        String firstCol = convertLExToStr(index.getIndexColumns().get(0));
        if (firstColIndexMap.get(firstCol) != null) {
          idxList = firstColIndexMap.get(firstCol);
        } else {
          idxList = new ArrayList<>();
        }
        idxList.add(index);
        firstColIndexMap.put(firstCol, idxList);
      }
    }
    for (String firstCol : firstColIndexMap.keySet()) {
      List<IndexDescriptor> indexesSameFirstCol = firstColIndexMap.get(firstCol);
      double maxAvgRowSize = -1.0;
      IndexDescriptor selectedIdx = null;
      for (IndexDescriptor idx : indexesSameFirstCol) {
        String tabIdxIdentifier = buildUniqueIndexIdentifier(idx);
        double idxRowSize = fIStatsCache.get(tabIdxIdentifier).getAvgRowSize();
        // Prefer index with largest average row-size, breaking ties lexicographically
        if (idxRowSize > maxAvgRowSize
            || (idxRowSize == maxAvgRowSize
                && (selectedIdx == null || idx.getIndexName().compareTo(selectedIdx.getIndexName()) < 0))) {
          maxAvgRowSize = idxRowSize;
          selectedIdx = idx;
        }
      }
      assert (selectedIdx != null);
      distinctIdxCollection.addIndex(selectedIdx);
    }
    return distinctIdxCollection;
  }

  /*
   * Returns the String representation for the given Logical Expression
   */
  private String convertLExToStr(LogicalExpression lex) {
    StringBuilder sb = new StringBuilder();
    ExpressionStringBuilder esb = new ExpressionStringBuilder();
    lex.accept(esb, sb);
    return sb.toString();
  }

  /*
   * Converts the given RexNode condition into a Drill logical expression.
   */
  private LogicalExpression convertToLogicalExpression(RexNode condition,
      RelDataType type, PlannerSettings settings, RexBuilder builder) {
    LogicalExpression conditionExp;
    try {
      conditionExp = DrillOptiq.toDrill(new DrillParseContext(settings), type, builder, condition);
    } catch (ClassCastException e) {
      return null;
    }
    return conditionExp;
  }
}
