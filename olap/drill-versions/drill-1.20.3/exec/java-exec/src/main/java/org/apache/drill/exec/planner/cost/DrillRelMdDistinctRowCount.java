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
package org.apache.drill.exec.planner.cost;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.SingleRel;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.core.Window;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMdDistinctRowCount;
import org.apache.calcite.rel.metadata.RelMdUtil;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.util.BuiltInMethod;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.planner.common.DrillJoinRelBase;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.metadata.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DrillRelMdDistinctRowCount extends RelMdDistinctRowCount{
  private static final Logger logger = LoggerFactory.getLogger(DrillRelMdDistinctRowCount.class);

  private static final DrillRelMdDistinctRowCount INSTANCE =
      new DrillRelMdDistinctRowCount();

  public static final RelMetadataProvider SOURCE =
      ReflectiveRelMetadataProvider.reflectiveSource(
          BuiltInMethod.DISTINCT_ROW_COUNT.method, INSTANCE);

  /**
   * We need to override this method since Calcite and Drill calculate
   * joined row count in different ways. It helps avoid a case when
   * at the first time was used Drill join row count but at the second time
   * Calcite row count was used. It may happen when
   * {@link RelMdDistinctRowCount#getDistinctRowCount(Join, RelMetadataQuery,
   * ImmutableBitSet, RexNode)} method is used and after that used
   * another getDistinctRowCount method for parent rel, which just uses
   * row count of input rel node (our join rel).
   * It causes cost increase of best rel node when
   * {@link RelSubset#propagateCostImprovements} is called.
   *
   * This is a part of the fix for CALCITE-2018.
   */
  @Override
  public Double getDistinctRowCount(Join rel, RelMetadataQuery mq,
      ImmutableBitSet groupKey, RexNode predicate) {
    return getDistinctRowCount((RelNode) rel, mq, groupKey, predicate);
  }

  @Override
  public Double getDistinctRowCount(RelNode rel, RelMetadataQuery mq, ImmutableBitSet groupKey, RexNode predicate) {
    if (rel instanceof TableScan) {                   // Applies to Calcite/Drill logical and Drill physical rels
      if (!DrillRelOptUtil.guessRows(rel)) {
        DrillTable table = Utilities.getDrillTable(rel.getTable());
        return getDistinctRowCountInternal(((TableScan) rel), mq, table, groupKey, rel.getRowType(), predicate);
      } else {
        /* If we are not using statistics OR there is no table or metadata (stats) table associated with scan,
         * estimate the distinct row count. Consistent with the estimation of Aggregate row count in
         * RelMdRowCount: distinctRowCount = rowCount * 10%.
         */
        if (rel instanceof DrillScanRel) {
          // The existing Drill behavior is to only use this estimation for DrillScanRel and not ScanPrel.
          // TODO: We may potentially do it for ScanPrel (outside the scope of statistics)
          return rel.estimateRowCount(mq) * 0.1;
        }
      }
    } else if (rel instanceof SingleRel && !DrillRelOptUtil.guessRows(rel)) {
      if (rel instanceof Window) {
        int childFieldCount = ((Window) rel).getInput().getRowType().getFieldCount();
        // For window aggregates delegate ndv to parent
        for (int bit : groupKey) {
          if (bit >= childFieldCount) {
            return super.getDistinctRowCount(rel, mq, groupKey, predicate);
          }
        }
      }
      return mq.getDistinctRowCount(((SingleRel) rel).getInput(), groupKey, predicate);
    } else if (rel instanceof DrillJoinRelBase && !DrillRelOptUtil.guessRows(rel)) {
      //Assume ndv is unaffected by the join
      return getDistinctRowCountInternal(((DrillJoinRelBase) rel), mq, groupKey, predicate);
    } else if (rel instanceof RelSubset && !DrillRelOptUtil.guessRows(rel)) {
      if (((RelSubset) rel).getBest() != null) {
        return mq.getDistinctRowCount(((RelSubset) rel).getBest(), groupKey, predicate);
      } else if (((RelSubset) rel).getOriginal() != null) {
        return mq.getDistinctRowCount(((RelSubset) rel).getOriginal(), groupKey, predicate);
      }
    }
    return super.getDistinctRowCount(rel, mq, groupKey, predicate);
  }

  /**
   * Estimates the number of rows which would be produced by a GROUP BY on the
   * set of columns indicated by groupKey.
   * column").
   */
  private Double getDistinctRowCountInternal(TableScan scan, RelMetadataQuery mq, DrillTable table,
      ImmutableBitSet groupKey, RelDataType type, RexNode predicate) {
    double selectivity, gbyColPredSel, rowCount;
    /* If predicate is present, determine its selectivity to estimate filtered rows.
     * Thereafter, compute the number of distinct rows.
     */
    selectivity = mq.getSelectivity(scan, predicate);
    rowCount = mq.getRowCount(scan);

    if (groupKey.length() == 0) {
      return selectivity * rowCount;
    }

    TableMetadata tableMetadata;
    try {
      tableMetadata = table.getGroupScan().getTableMetadata();
    } catch (IOException e) {
      // Statistics cannot be obtained, use default behaviour
      return scan.estimateRowCount(mq) * 0.1;
    }

    double estRowCnt = 1.0;
    String colName = "";
    boolean allColsHaveNDV = true;
    for (int i = 0; i < groupKey.length(); i++) {
      colName = type.getFieldNames().get(i);
      if (!groupKey.get(i)) {
        continue;
      }
      ColumnStatistics<?> columnStatistics = tableMetadata != null ?
          tableMetadata.getColumnStatistics(SchemaPath.getSimplePath(colName)) : null;
      Double ndv = columnStatistics != null ? ColumnStatisticsKind.NDV.getFrom(columnStatistics) : null;
      // Skip NDV, if not available
      if (ndv == null) {
        allColsHaveNDV = false;
        break;
      }
      estRowCnt *= ndv;
      gbyColPredSel = getPredSelectivityContainingInputRef(predicate, i, mq, scan);
      /* If predicate is on group-by column, scale down the NDV by selectivity. Consider the query
       * select a, b from t where a = 10 group by a, b. Here, NDV(a) will be scaled down by SEL(a)
       * whereas NDV(b) will not.
       */
      if (gbyColPredSel > 0) {
        estRowCnt *= gbyColPredSel;
      }
    }
    // Estimated NDV should not exceed number of rows after applying the filters
    estRowCnt = Math.min(estRowCnt, selectivity*rowCount);
    if (!allColsHaveNDV) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("NDV not available for %s(%s). Using default rowcount for group-by %s",
            (tableMetadata != null ? tableMetadata.getTableInfo().name() : ""), colName, groupKey.toString()));
      }
      // Could not get any NDV estimate from stats - probably stats not present for GBY cols. So Guess!
      return scan.estimateRowCount(mq) * 0.1;
    } else {
    /* rowCount maybe less than NDV(different source), sanity check OR NDV not used at all */
      return estRowCnt;
    }
  }

  private Double getDistinctRowCountInternal(DrillJoinRelBase joinRel, RelMetadataQuery mq, ImmutableBitSet groupKey,
       RexNode predicate) {
    if (DrillRelOptUtil.guessRows(joinRel)) {
      return super.getDistinctRowCount(joinRel, mq, groupKey, predicate);
    }
    // Assume NDV is unaffected by the join when groupKey comes from one side of the join
    // Alleviates NDV over-estimates
    ImmutableBitSet.Builder leftMask = ImmutableBitSet.builder();
    ImmutableBitSet.Builder rightMask = ImmutableBitSet.builder();
    JoinRelType joinType = joinRel.getJoinType();
    RelNode left = joinRel.getInputs().get(0);
    RelNode right = joinRel.getInputs().get(1);
    RelMdUtil.setLeftRightBitmaps(groupKey, leftMask, rightMask,
        left.getRowType().getFieldCount());
    RexNode leftPred = null;
    RexNode rightPred = null;

    // Identify predicates which can be pushed onto the left and right sides of the join
    if (predicate != null) {
      List<RexNode> leftFilters = new ArrayList<>();
      List<RexNode> rightFilters = new ArrayList<>();
      List<RexNode> joinFilters = new ArrayList();
      List<RexNode> predList = RelOptUtil.conjunctions(predicate);
      RelOptUtil.classifyFilters(joinRel, predList, joinType, joinType == JoinRelType.INNER,
          !joinType.generatesNullsOnLeft(), !joinType.generatesNullsOnRight(), joinFilters,
              leftFilters, rightFilters);
      RexBuilder rexBuilder = joinRel.getCluster().getRexBuilder();
      leftPred = RexUtil.composeConjunction(rexBuilder, leftFilters, true);
      rightPred = RexUtil.composeConjunction(rexBuilder, rightFilters, true);
    }

    double distRowCount = 1;
    int gbyCols = 0;
    PlannerSettings plannerSettings = PrelUtil.getPlannerSettings(joinRel.getCluster().getPlanner());
    /*
     * The NDV for a multi-column GBY key past a join is determined as follows:
     * GBY(s1, s2, s3) = CNDV(s1)*CNDV(s2)*CNDV(s3)
     * where CNDV is determined as follows:
     * A) If sX is present as a join column (sX = tX) CNDV(sX) = MIN(NDV(sX), NDV(tX)) where X =1, 2, 3, etc
     * B) Otherwise, based on independence assumption CNDV(sX) = NDV(sX)
     */
    Set<ImmutableBitSet> joinFiltersSet = new HashSet<>();
    for (RexNode filter : RelOptUtil.conjunctions(joinRel.getCondition())) {
      final RelOptUtil.InputFinder inputFinder = RelOptUtil.InputFinder.analyze(filter);
      joinFiltersSet.add(inputFinder.inputBitSet.build());
    }
    for (int idx = 0; idx < groupKey.length(); idx++) {
      if (groupKey.get(idx)) {
        // GBY key is present in some filter - now try options A) and B) as described above
        double ndvSGby = Double.MAX_VALUE;
        Double ndv;
        boolean presentInFilter = false;
        ImmutableBitSet sGby = getSingleGbyKey(groupKey, idx);
        if (sGby != null) {
          // If we see any NULL ndv i.e. cant process ..we bail out!
          for (ImmutableBitSet jFilter : joinFiltersSet) {
            if (jFilter.contains(sGby)) {
              presentInFilter = true;
              // Found join condition containing this GBY key. Pick min NDV across all columns in this join
              for (int fidx : jFilter) {
                if (fidx < left.getRowType().getFieldCount()) {
                  ndv = mq.getDistinctRowCount(left, ImmutableBitSet.of(fidx), leftPred);
                  if (ndv == null) {
                    return super.getDistinctRowCount(joinRel, mq, groupKey, predicate);
                  }
                  ndvSGby = Math.min(ndvSGby, ndv);
                } else {
                  ndv = mq.getDistinctRowCount(right, ImmutableBitSet.of(fidx-left.getRowType().getFieldCount()), rightPred);
                  if (ndv == null) {
                    return super.getDistinctRowCount(joinRel, mq, groupKey, predicate);
                  }
                  ndvSGby = Math.min(ndvSGby, ndv);
                }
              }
              break;
            }
          }
          // Did not find it in any join condition(s)
          if (!presentInFilter) {
            for (int sidx : sGby) {
              if (sidx < left.getRowType().getFieldCount()) {
                ndv = mq.getDistinctRowCount(left, ImmutableBitSet.of(sidx), leftPred);
                if (ndv == null) {
                  return super.getDistinctRowCount(joinRel, mq, groupKey, predicate);
                }
                ndvSGby = ndv;
              } else {
                ndv = mq.getDistinctRowCount(right, ImmutableBitSet.of(sidx-left.getRowType().getFieldCount()), rightPred);
                if (ndv == null) {
                  return super.getDistinctRowCount(joinRel, mq, groupKey, predicate);
                }
                ndvSGby = ndv;
              }
            }
          }
          ++gbyCols;
          // Multiply NDV(s) of different GBY cols to determine the overall NDV
          distRowCount *= ndvSGby;
        }
      }
    }
    if (gbyCols > 1) { // Scale with multi-col NDV factor if more than one GBY cols were found
      distRowCount *= plannerSettings.getStatisticsMultiColNdvAdjustmentFactor();
    }
    double joinRowCount = mq.getRowCount(joinRel);
    // Cap NDV to join row count
    distRowCount = Math.min(distRowCount, joinRowCount);
    return RelMdUtil.numDistinctVals(distRowCount, joinRowCount);
  }

  private ImmutableBitSet getSingleGbyKey(ImmutableBitSet groupKey, int idx) {
    if (groupKey.get(idx)) {
      return ImmutableBitSet.builder().set(idx, idx+1).build();
    } else {
      return null;
    }
  }

  private double getPredSelectivityContainingInputRef(RexNode predicate, int inputRef,
      RelMetadataQuery mq, TableScan scan) {
    if (predicate instanceof RexCall) {
      if (predicate.getKind() == SqlKind.AND) {
        double sel, andSel = 1.0;
        for (RexNode op : ((RexCall) predicate).getOperands()) {
          sel = getPredSelectivityContainingInputRef(op, inputRef, mq, scan);
          if (sel > 0) {
            andSel *= sel;
          }
        }
        return andSel;
      } else if (predicate.getKind() == SqlKind.OR) {
        double sel, orSel = 0.0;
        for (RexNode op : ((RexCall) predicate).getOperands()) {
          sel = getPredSelectivityContainingInputRef(op, inputRef, mq, scan);
          if (sel > 0) {
            orSel += sel;
          }
        }
        return orSel;
      } else {
        for (RexNode op : ((RexCall) predicate).getOperands()) {
          if (op instanceof RexInputRef && inputRef != ((RexInputRef) op).getIndex()) {
            return -1.0;
          }
        }
        return mq.getSelectivity(scan, predicate);
      }
    } else {
      return -1.0;
    }
  }

  @Override
  public Double getDistinctRowCount(RelSubset rel, RelMetadataQuery mq,
      ImmutableBitSet groupKey, RexNode predicate) {
    if (!DrillRelOptUtil.guessRows(rel)) {
      final RelNode best = rel.getBest();
      if (best != null) {
        return mq.getDistinctRowCount(best, groupKey, predicate);
      }
      final RelNode original = rel.getOriginal();
      if (original != null) {
        return mq.getDistinctRowCount(original, groupKey, predicate);
      }
    }
    return super.getDistinctRowCount(rel, mq, groupKey, predicate);
  }
}
