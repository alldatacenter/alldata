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
package org.apache.drill.exec.planner.physical;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.exec.planner.logical.DrillAggregateRel;
import org.apache.drill.exec.planner.logical.DrillProjectRel;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.common.CountToDirectScanUtils;

import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.direct.MetadataDirectGroupScan;
import org.apache.drill.exec.store.pojo.DynamicPojoRecordReader;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This rule will convert <b>" select count(*)  as mycount from table "</b>
 * or <b>" select count(not-nullable-expr) as mycount from table "</b> into
 * <pre>
 *    Project(mycount)
 *         \
 *    DirectGroupScan ( PojoRecordReader ( rowCount ))
 *</pre>
 * or <b>" select count(column) as mycount from table "</b> into
 * <pre>
 *      Project(mycount)
 *           \
 *            DirectGroupScan (PojoRecordReader (columnValueCount))
 *</pre>
 * Rule can be applied if query contains multiple count expressions.
 * <b>" select count(column1), count(column2), count(*) from table "</b>
 * </p>
 *
 * <p>
 * Currently, only parquet group scan has the exact row count and column value count,
 * obtained from parquet row group info. This will save the cost to
 * scan the whole parquet files.
 * </p>
 *
 * <p>
 *     NOTE: This rule is a physical planning counterpart to a similar ConvertCountToDirectScanRule
 *     logical rule. However, while the logical rule relies on the Parquet metadata cache's Summary
 *     aggregates, this rule is applicable if the exact row count is available from the GroupScan
 *     regardless of where that stat came from. Hence, it is more general, with the trade-off that the
 *     GroupScan relies on the fully expanded list of row groups to compute the aggregate row count.
 * </p>
 */
public class ConvertCountToDirectScanPrule extends Prule {

  public static final RelOptRule AGG_ON_PROJ_ON_SCAN = new ConvertCountToDirectScanPrule(
      RelOptHelper.some(DrillAggregateRel.class,
                        RelOptHelper.some(DrillProjectRel.class,
                            RelOptHelper.any(DrillScanRel.class))), "Agg_on_proj_on_scan");

  public static final RelOptRule AGG_ON_SCAN = new ConvertCountToDirectScanPrule(
      RelOptHelper.some(DrillAggregateRel.class,
                            RelOptHelper.any(DrillScanRel.class)), "Agg_on_scan");

  private static final Logger logger = LoggerFactory.getLogger(ConvertCountToDirectScanPrule.class);

  private ConvertCountToDirectScanPrule(RelOptRuleOperand rule, String id) {
    super(rule, "ConvertCountToDirectScanPrule:" + id);
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final DrillAggregateRel agg = call.rel(0);
    final DrillScanRel scan = call.rel(call.rels.length - 1);
    final DrillProjectRel project = call.rels.length == 3 ? call.rel(1) : null;

    final GroupScan oldGrpScan = scan.getGroupScan();
    final PlannerSettings settings = PrelUtil.getPlannerSettings(call.getPlanner());

    // Only apply the rule when:
    //    1) scan knows the exact row count in getSize() call,
    //    2) No GroupBY key,
    //    3) No distinct agg call.
    if (!(oldGrpScan.getScanStats(settings).getGroupScanProperty().hasExactRowCount()
        && agg.getGroupCount() == 0
        && !agg.containsDistinctCall())) {
      return;
    }

    Map<String, Long> result = collectCounts(settings, agg, scan, project);
    logger.trace("Calculated the following aggregate counts: {}", result);
    // if could not determine the counts, rule won't be applied
    if (result.isEmpty()) {
      return;
    }

    final RelDataType scanRowType = CountToDirectScanUtils.constructDataType(agg, result.keySet());

    final DynamicPojoRecordReader<Long> reader = new DynamicPojoRecordReader<>(
        CountToDirectScanUtils.buildSchema(scanRowType.getFieldNames()),
        Collections.singletonList(new ArrayList<>(result.values())));

    final ScanStats scanStats = new ScanStats(ScanStats.GroupScanProperty.EXACT_ROW_COUNT, 1, 1, scanRowType.getFieldCount());
    final int numFiles = oldGrpScan.hasFiles() ? oldGrpScan.getFiles().size() : -1;
    final GroupScan directScan = new MetadataDirectGroupScan(reader, oldGrpScan.getSelectionRoot(), numFiles, scanStats, false, oldGrpScan.usedMetastore());

    final DirectScanPrel newScan = DirectScanPrel.create(scan, scan.getTraitSet().plus(Prel.DRILL_PHYSICAL)
        .plus(DrillDistributionTrait.SINGLETON), directScan, scanRowType);

    final ProjectPrel newProject = new ProjectPrel(agg.getCluster(), agg.getTraitSet().plus(Prel.DRILL_PHYSICAL)
        .plus(DrillDistributionTrait.SINGLETON), newScan, CountToDirectScanUtils.prepareFieldExpressions(scanRowType), agg.getRowType());

    call.transformTo(newProject);
  }

  /**
   * Collects counts for each aggregation call.
   * Will return empty result map if was not able to determine count for at least one aggregation call,
   *
   * For each aggregate call will determine if count can be calculated. Collects counts only for COUNT function.
   * For star, not null expressions and implicit columns sets count to total record number.
   * For other cases obtains counts from group scan operator. Also count can not be calculated for partition columns.
   *
   * @param agg aggregate relational expression
   * @param scan scan relational expression
   * @param project project relational expression
   * @return result map where key is count column name, value is count value
   */
  private Map<String, Long> collectCounts(PlannerSettings settings, DrillAggregateRel agg, DrillScanRel scan, DrillProjectRel project) {
    final Set<String> implicitColumnsNames = ColumnExplorer.initImplicitFileColumns(settings.getOptions()).keySet();
    final GroupScan oldGrpScan = scan.getGroupScan();
    final long totalRecordCount = (long) oldGrpScan.getScanStats(settings).getRecordCount();
    final LinkedHashMap<String, Long> result = new LinkedHashMap<>();

    for (int i = 0; i < agg.getAggCallList().size(); i++) {
      AggregateCall aggCall = agg.getAggCallList().get(i);
      long cnt;

      // rule can be applied only for count function, return empty counts
      if (!"count".equalsIgnoreCase(aggCall.getAggregation().getName()) ) {
        return ImmutableMap.of();
      }

      if (CountToDirectScanUtils.containsStarOrNotNullInput(aggCall, agg)) {
        cnt = totalRecordCount;

      } else if (aggCall.getArgList().size() == 1) {
        // count(columnName) ==> Agg ( Scan )) ==> columnValueCount
        int index = aggCall.getArgList().get(0);

        if (project != null) {
          // project in the middle of Agg and Scan : Only when input of AggCall is a RexInputRef in Project, we find the index of Scan's field.
          // For instance,
          // Agg - count($0)
          //  \
          //  Proj - Exp={$1}
          //    \
          //   Scan (col1, col2).
          // return count of "col2" in Scan's metadata, if found.
          if (!(project.getProjects().get(index) instanceof RexInputRef)) {
            return ImmutableMap.of(); // do not apply for all other cases.
          }

          index = ((RexInputRef) project.getProjects().get(index)).getIndex();
        }

        String columnName = scan.getRowType().getFieldNames().get(index).toLowerCase();

        // for implicit column count will the same as total record count
        if (implicitColumnsNames.contains(columnName)) {
          cnt = totalRecordCount;
        } else {
          SchemaPath simplePath = SchemaPath.getSimplePath(columnName);

          if (ColumnExplorer.isPartitionColumn(settings.getOptions(), simplePath)) {
            return ImmutableMap.of();
          }

          cnt = oldGrpScan.getColumnValueCount(simplePath);
          if (cnt == Statistic.NO_COLUMN_STATS) {
            // if column stats is not available don't apply this rule, return empty counts
            return ImmutableMap.of();
          }
        }
      } else {
        return ImmutableMap.of();
      }

      String name = "count" + i + "$" + (aggCall.getName() == null ? aggCall.toString() : aggCall.getName());
      result.put(name, cnt);
    }

    return ImmutableMap.copyOf(result);
  }
}
