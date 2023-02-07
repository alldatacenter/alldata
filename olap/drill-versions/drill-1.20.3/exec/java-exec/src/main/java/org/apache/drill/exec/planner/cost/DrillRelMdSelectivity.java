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
import java.util.EnumSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import java.util.stream.Collectors;

import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMdSelectivity;
import org.apache.calcite.rel.metadata.RelMdUtil;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.rex.RexVisitor;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.util.BuiltInMethod;
import org.apache.calcite.util.Util;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.common.DrillJoinRelBase;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.metastore.statistics.Histogram;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.metadata.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DrillRelMdSelectivity extends RelMdSelectivity {
  private static final Logger logger = LoggerFactory.getLogger(DrillRelMdSelectivity.class);

  private static final DrillRelMdSelectivity INSTANCE = new DrillRelMdSelectivity();
  public static final RelMetadataProvider SOURCE = ReflectiveRelMetadataProvider.reflectiveSource(BuiltInMethod.SELECTIVITY.method, INSTANCE);
  /*
   * For now, we are treating all LIKE predicates to have the same selectivity irrespective of the number or position
   * of wildcard characters (%). This is no different than the present Drill/Calcite behaviour w.r.t to LIKE predicates.
   * The difference being Calcite keeps the selectivity 25% whereas we keep it at 5%
   * TODO: Differentiate leading/trailing wildcard characters(%) or explore different estimation techniques e.g. LSH-based
   */
  private static final double LIKE_PREDICATE_SELECTIVITY = 0.05;

  public static final Set<SqlKind> RANGE_PREDICATE =
    EnumSet.of(
      SqlKind.LESS_THAN, SqlKind.GREATER_THAN,
      SqlKind.LESS_THAN_OR_EQUAL, SqlKind.GREATER_THAN_OR_EQUAL);

  @Override
  public Double getSelectivity(RelNode rel, RelMetadataQuery mq, RexNode predicate) {
    if (rel instanceof RelSubset && !DrillRelOptUtil.guessRows(rel)) {
      return getSubsetSelectivity((RelSubset) rel, mq, predicate);
    } else if (rel instanceof TableScan) {
      return getScanSelectivity(rel, mq, predicate);
    } else if (rel instanceof DrillJoinRelBase) {
      return getJoinSelectivity(((DrillJoinRelBase) rel), mq, predicate);
    } /*else if (rel instanceof SingleRel && !DrillRelOptUtil.guessRows(rel)) {
      return getSelectivity(((SingleRel)rel).getInput(), mq, predicate);
    }*/ else {
      return super.getSelectivity(rel, mq, predicate);
    }
  }

  private Double getSubsetSelectivity(RelSubset rel, RelMetadataQuery mq, RexNode predicate) {
    if (rel.getBest() != null) {
      return getSelectivity(rel.getBest(), mq, predicate);
    } else {
      List<RelNode> list = rel.getRelList();
      if (list != null && list.size() > 0) {
        return getSelectivity(list.get(0), mq, predicate);
      }
    }
    //TODO: Not required? return mq.getSelectivity(((RelSubset)rel).getOriginal(), predicate);
    return RelMdUtil.guessSelectivity(predicate);
  }

  private Double getScanSelectivity(RelNode rel, RelMetadataQuery mq, RexNode predicate) {
    double ROWCOUNT_UNKNOWN = -1.0;
    GroupScan scan = null;
    PlannerSettings settings = PrelUtil.getPlannerSettings(rel.getCluster().getPlanner());
    final RexBuilder rexBuilder = rel.getCluster().getRexBuilder();

    if (rel instanceof DrillScanRel) {
      scan = ((DrillScanRel) rel).getGroupScan();
    } else if (rel instanceof ScanPrel) {
      scan = ((ScanPrel) rel).getGroupScan();
    }
    if (scan != null) {
      if (settings.isStatisticsEnabled()
          && scan instanceof DbGroupScan) {
        double filterRows = ((DbGroupScan) scan).getRowCount(predicate, rel);
        double totalRows = ((DbGroupScan) scan).getRowCount(null, rel);
        if (filterRows != ROWCOUNT_UNKNOWN &&
            totalRows != ROWCOUNT_UNKNOWN && totalRows > 0) {
          return Math.min(1.0, filterRows / totalRows);
        }
      }
    }
    // Do not mess with statistics used for DBGroupScans.
    if (rel instanceof TableScan) {
      if (DrillRelOptUtil.guessRows(rel)) {
        return super.getSelectivity(rel, mq, predicate);
      }
      DrillTable table = Utilities.getDrillTable(rel.getTable());
      try {
        TableMetadata tableMetadata;
        if (table != null && (tableMetadata = table.getGroupScan().getTableMetadata()) != null
            && TableStatisticsKind.HAS_DESCRIPTIVE_STATISTICS.getValue(tableMetadata)) {
          List<SchemaPath> fieldNames;
          if (rel instanceof DrillScanRelBase) {
            fieldNames = ((DrillScanRelBase) rel).getGroupScan().getColumns();
          } else {
            fieldNames = rel.getRowType().getFieldNames().stream()
                .map(SchemaPath::getSimplePath)
                .collect(Collectors.toList());
          }
          return getScanSelectivityInternal(tableMetadata, predicate, fieldNames, rexBuilder);
        }
      } catch (IOException e) {
        super.getSelectivity(rel, mq, predicate);
      }
    }
    return super.getSelectivity(rel, mq, predicate);
  }

  private double getScanSelectivityInternal(TableMetadata tableMetadata, RexNode predicate, List<SchemaPath> fieldNames, RexBuilder rexBuilder) {
    double sel = 1.0;
    if ((predicate == null) || predicate.isAlwaysTrue()) {
      return sel;
    }

    List<RexNode> conjuncts1 = RelOptUtil.conjunctions(predicate);

    // a Set that holds range predicates that are combined based on whether they are defined on the same column
    Set<RexNode> combinedRangePredicates = new HashSet<>();
    // pre-process the conjuncts such that predicates on the same column are grouped together
    List<RexNode> conjuncts2 = preprocessRangePredicates(conjuncts1, fieldNames, rexBuilder, combinedRangePredicates);

    for (RexNode pred : conjuncts2) {
      double orSel = 0;
      for (RexNode orPred : RelOptUtil.disjunctions(pred)) {
        if (isMultiColumnPredicate(orPred) && !combinedRangePredicates.contains(orPred)) {
          Set uniqueRefs = new HashSet<>();
          uniqueRefs.add(DrillRelOptUtil.findAllRexInputRefs(orPred));
          // If equality predicate involving single column - selectivity is 1.0
          if (uniqueRefs.size() == 1) {
            try {
              RexVisitor<Void> visitor =
                      new RexVisitorImpl<Void>(true) {
                        @Override
                        public Void visitCall(RexCall call) {
                          if (call.getKind() != SqlKind.EQUALS) {
                            throw new Util.FoundOne(call);
                          }
                          return super.visitCall(call);
                        }
                      };
              pred.accept(visitor);
              orSel += 1.0;
            } catch (Util.FoundOne e) {
              orSel += RelMdUtil.guessSelectivity(orPred);  //CALCITE guess
            }
          }
        } else if (orPred.isA(SqlKind.EQUALS)) {
          orSel += computeEqualsSelectivity(tableMetadata, orPred, fieldNames);
        } else if (orPred.isA(RANGE_PREDICATE) || combinedRangePredicates.contains(orPred)) {
          orSel += computeRangeSelectivity(tableMetadata, orPred, fieldNames);
        } else if (orPred.isA(SqlKind.NOT_EQUALS)) {
          orSel += 1.0 - computeEqualsSelectivity(tableMetadata, orPred, fieldNames);
        } else if (orPred.isA(SqlKind.LIKE)) {
          // LIKE selectivity is 5% more than a similar equality predicate, capped at CALCITE guess
          orSel +=  Math.min(computeEqualsSelectivity(tableMetadata, orPred, fieldNames) + LIKE_PREDICATE_SELECTIVITY,
              guessSelectivity(orPred));
        } else if (orPred.isA(SqlKind.NOT)) {
          if (orPred instanceof RexCall) {
            // LIKE selectivity is 5% more than a similar equality predicate, capped at CALCITE guess
            RexNode childOp = ((RexCall) orPred).getOperands().get(0);
            if (childOp.isA(SqlKind.LIKE)) {
              orSel += 1.0 - Math.min(computeEqualsSelectivity(tableMetadata, childOp, fieldNames) + LIKE_PREDICATE_SELECTIVITY,
                      guessSelectivity(childOp));
            } else {
              orSel += 1.0 - guessSelectivity(orPred);
            }
          }
        } else if (orPred.isA(SqlKind.IS_NULL)) {
          orSel += 1.0 - computeIsNotNullSelectivity(tableMetadata, orPred, fieldNames);
        } else if (orPred.isA(SqlKind.IS_NOT_NULL)) {
          orSel += computeIsNotNullSelectivity(tableMetadata, orPred, fieldNames);
        } else {
          // Use the CALCITE guess.
          orSel += guessSelectivity(orPred);
        }
      }
      sel *= orSel;
    }
    // Cap selectivity if it exceeds 1.0
    return (sel > 1.0) ? 1.0 : sel;
  }

  /**
   * Process the range predicates and combine all range predicates defined on the same column into a single conjunct.
   * <p>
   * For example:  a > 10 AND b < 50 AND c > 20 AND a < 70
   * Will be combined into 3 conjuncts: (a > 10 AND a < 70), (b < 50), (c > 20)
   * </p>
   *
   * @param conjuncts
   * @param fieldNames
   * @param rexBuilder
   * @param combinedRangePredicates
   * @return A list of predicates that includes the newly created predicate
   */
  private List<RexNode> preprocessRangePredicates(List<RexNode> conjuncts, List<SchemaPath> fieldNames, RexBuilder rexBuilder,
                                                  Set<RexNode> combinedRangePredicates) {
    Map<SchemaPath, List<RexNode>> colToRangePredicateMap = new HashMap<>();
    List<RexNode> nonRangePredList = new ArrayList<RexNode>();
    for (RexNode pred : conjuncts) {
      if (pred.isA(RANGE_PREDICATE)) {
        SchemaPath col = getColumn(pred, fieldNames);
        if (col != null) {
          List<RexNode> predList = null;
          if ((predList = colToRangePredicateMap.get(col)) != null) {
            predList.add(pred);
          } else {
            predList = new ArrayList<>();
            predList.add(pred);
            colToRangePredicateMap.put(col, predList);
          }
        }
      } else {
        nonRangePredList.add(pred);
      }
    }

    List<RexNode> newPredsList = new ArrayList<>();
    newPredsList.addAll(nonRangePredList);

    // for the predicates on same column, combine them into a single conjunct
    for (Map.Entry<SchemaPath, List<RexNode>> entry : colToRangePredicateMap.entrySet()) {
      List<RexNode> predList = entry.getValue();
      if (predList.size() >= 1) {
        if (predList.size() > 1) {
          RexNode newPred = RexUtil.composeConjunction(rexBuilder, predList, false);
          newPredsList.add(newPred);
          // also save this newly created predicate in a separate set for later use
          combinedRangePredicates.add(newPred);
        } else {
          newPredsList.add(predList.get(0));
        }
      }
    }
    return newPredsList;
  }

  private double computeEqualsSelectivity(TableMetadata tableMetadata, RexNode orPred, List<SchemaPath> fieldNames) {
    SchemaPath col = getColumn(orPred, fieldNames);
    if (col != null) {
      ColumnStatistics<?> columnStatistics = tableMetadata != null ? tableMetadata.getColumnStatistics(col) : null;
      Double ndv = columnStatistics != null ? ColumnStatisticsKind.NDV.getFrom(columnStatistics) : null;
      if (ndv != null) {
        return 1.00 / ndv;
      }
    }
    return guessSelectivity(orPred);
  }

  // Use histogram if available for the range predicate selectivity
  private double computeRangeSelectivity(TableMetadata tableMetadata, RexNode orPred, List<SchemaPath> fieldNames) {
    SchemaPath col = getColumn(orPred, fieldNames);
    if (col != null) {
      ColumnStatistics<?> columnStatistics = tableMetadata != null ? tableMetadata.getColumnStatistics(col) : null;
      Histogram histogram = columnStatistics != null ? ColumnStatisticsKind.HISTOGRAM.getFrom(columnStatistics) : null;
      if (histogram != null) {
        Double totalCount = ColumnStatisticsKind.ROWCOUNT.getFrom(columnStatistics);
        Double ndv = ColumnStatisticsKind.NDV.getFrom(columnStatistics);
        Double sel = histogram.estimatedSelectivity(orPred, totalCount.longValue(), ndv.longValue());
        if (sel != null) {
          return sel;
        }
      }
    }
    return guessSelectivity(orPred);
  }

  private double computeIsNotNullSelectivity(TableMetadata tableMetadata, RexNode orPred, List<SchemaPath> fieldNames) {
    SchemaPath col = getColumn(orPred, fieldNames);
    if (col != null) {
      ColumnStatistics<?> columnStatistics = tableMetadata != null ? tableMetadata.getColumnStatistics(col) : null;
      Double nonNullCount = columnStatistics != null ? ColumnStatisticsKind.NON_NULL_COUNT.getFrom(columnStatistics) : null;
      if (nonNullCount != null) {
        // Cap selectivity below Calcite Guess
        return Math.min(nonNullCount / TableStatisticsKind.EST_ROW_COUNT.getValue(tableMetadata),
            RelMdUtil.guessSelectivity(orPred));
      }
    }
    return guessSelectivity(orPred);
  }

  private SchemaPath getColumn(RexNode orPred, List<SchemaPath> fieldNames) {
    if (orPred instanceof RexCall) {
      int colIdx = -1;
      RexInputRef op = findRexInputRef(orPred);
      if (op != null) {
        colIdx = op.getIndex();
      }
      if (colIdx != -1 && colIdx < fieldNames.size()) {
        return fieldNames.get(colIdx);
      } else {
        if (logger.isDebugEnabled()) {
          logger.warn(String.format("No input reference $[%s] found for predicate [%s]",
                  Integer.toString(colIdx), orPred.toString()));
        }
      }
    }
    return null;
  }

  private double guessSelectivity(RexNode orPred) {
    if (logger.isDebugEnabled()) {
      logger.warn(String.format("Using guess for predicate [%s]", orPred.toString()));
    }
    //CALCITE guess
    return RelMdUtil.guessSelectivity(orPred);
  }

  private Double getJoinSelectivity(DrillJoinRelBase rel, RelMetadataQuery mq, RexNode predicate) {
    double sel = 1.0;
    // determine which filters apply to the left vs right
    RexNode leftPred, rightPred;
    JoinRelType joinType = rel.getJoinType();
    final RexBuilder rexBuilder = rel.getCluster().getRexBuilder();
    int[] adjustments = new int[rel.getRowType().getFieldCount()];

    if (DrillRelOptUtil.guessRows(rel)) {
      return super.getSelectivity(rel, mq, predicate);
    }

    if (predicate != null) {
      RexNode pred;
      List<RexNode> leftFilters = new ArrayList<>();
      List<RexNode> rightFilters = new ArrayList<>();
      List<RexNode> joinFilters = new ArrayList<>();
      List<RexNode> predList = RelOptUtil.conjunctions(predicate);

      RelOptUtil.classifyFilters(
          rel,
          predList,
          joinType,
          joinType == JoinRelType.INNER,
          !joinType.generatesNullsOnLeft(),
          !joinType.generatesNullsOnRight(),
          joinFilters,
          leftFilters,
          rightFilters);
      leftPred =
          RexUtil.composeConjunction(rexBuilder, leftFilters, true);
      rightPred =
          RexUtil.composeConjunction(rexBuilder, rightFilters, true);
      for (RelNode child : rel.getInputs()) {
        RexNode modifiedPred = null;

        if (child == rel.getLeft()) {
          pred = leftPred;
        } else {
          pred = rightPred;
        }
        if (pred != null) {
          // convert the predicate to reference the types of the children
          modifiedPred =
              pred.accept(new RelOptUtil.RexInputConverter(
              rexBuilder,
              null,
              child.getRowType().getFieldList(),
              adjustments));
        }
        sel *= mq.getSelectivity(child, modifiedPred);
      }
      sel *= RelMdUtil.guessSelectivity(RexUtil.composeConjunction(rexBuilder, joinFilters, true));
    }
    return sel;
  }

  private static RexInputRef findRexInputRef(final RexNode node) {
    try {
      RexVisitor<Void> visitor =
          new RexVisitorImpl<Void>(true) {
            @Override
            public Void visitCall(RexCall call) {
              for (RexNode child : call.getOperands()) {
                child.accept(this);
              }
              return super.visitCall(call);
            }

            @Override
            public Void visitInputRef(RexInputRef inputRef) {
              throw new Util.FoundOne(inputRef);
            }
          };
      node.accept(visitor);
      return null;
    } catch (Util.FoundOne e) {
      Util.swallow(e, null);
      return (RexInputRef) e.getNode();
    }
  }

  private boolean isMultiColumnPredicate(final RexNode node) {
    return DrillRelOptUtil.findAllRexInputRefs(node).size() > 1;
  }
}
