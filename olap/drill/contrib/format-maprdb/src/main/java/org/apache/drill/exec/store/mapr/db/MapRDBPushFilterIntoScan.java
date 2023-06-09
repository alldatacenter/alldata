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
package org.apache.drill.exec.store.mapr.db;

import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.planner.logical.DrillOptiq;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.FilterPrel;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.hbase.HBaseScanSpec;
import org.apache.drill.exec.store.mapr.db.binary.BinaryTableGroupScan;
import org.apache.drill.exec.store.mapr.db.binary.MapRDBFilterBuilder;
import org.apache.drill.exec.store.mapr.db.json.JsonConditionBuilder;
import org.apache.drill.exec.store.mapr.db.json.JsonScanSpec;
import org.apache.drill.exec.store.mapr.db.json.JsonTableGroupScan;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

public abstract class MapRDBPushFilterIntoScan extends StoragePluginOptimizerRule {

  private MapRDBPushFilterIntoScan(RelOptRuleOperand operand, String description) {
    super(operand, description);
  }

  public static final StoragePluginOptimizerRule FILTER_ON_SCAN = new MapRDBPushFilterIntoScan(RelOptHelper.some(FilterPrel.class, RelOptHelper.any(ScanPrel.class)), "MapRDBPushFilterIntoScan:Filter_On_Scan") {

    @Override
    public void onMatch(RelOptRuleCall call) {
      final FilterPrel filter = call.rel(0);
      final ScanPrel scan = call.rel(1);

      final RexNode condition = filter.getCondition();

      if (scan.getGroupScan() instanceof BinaryTableGroupScan) {
        BinaryTableGroupScan groupScan = (BinaryTableGroupScan)scan.getGroupScan();
        doPushFilterIntoBinaryGroupScan(call, filter, null, scan, groupScan, condition);
      } else {
        assert(scan.getGroupScan() instanceof JsonTableGroupScan);
        JsonTableGroupScan groupScan = (JsonTableGroupScan)scan.getGroupScan();
        doPushFilterIntoJsonGroupScan(call, filter, null, scan, groupScan, condition);
      }
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      final ScanPrel scan = (ScanPrel) call.rel(1);
      if (scan.getGroupScan() instanceof BinaryTableGroupScan ||
          scan.getGroupScan() instanceof JsonTableGroupScan) {
        return super.matches(call);
      }
      return false;
    }
  };

  public static final StoragePluginOptimizerRule FILTER_ON_PROJECT = new MapRDBPushFilterIntoScan(RelOptHelper.some(FilterPrel.class, RelOptHelper.some(ProjectPrel.class, RelOptHelper.any(ScanPrel.class))), "MapRDBPushFilterIntoScan:Filter_On_Project") {

    @Override
    public void onMatch(RelOptRuleCall call) {
      final FilterPrel filter = call.rel(0);
      final ProjectPrel project = call.rel(1);
      final ScanPrel scan = call.rel(2);

      // convert the filter to one that references the child of the project
      final RexNode condition =  RelOptUtil.pushPastProject(filter.getCondition(), project);

      if (scan.getGroupScan() instanceof BinaryTableGroupScan) {
        BinaryTableGroupScan groupScan = (BinaryTableGroupScan)scan.getGroupScan();
        doPushFilterIntoBinaryGroupScan(call, filter, project, scan, groupScan, condition);
      } else {
        assert(scan.getGroupScan() instanceof JsonTableGroupScan);
        JsonTableGroupScan groupScan = (JsonTableGroupScan)scan.getGroupScan();
        doPushFilterIntoJsonGroupScan(call, filter, project, scan, groupScan, condition);
      }
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
      final ScanPrel scan = call.rel(2);
      if (scan.getGroupScan() instanceof BinaryTableGroupScan ||
          scan.getGroupScan() instanceof JsonTableGroupScan) {
        return super.matches(call);
      }
      return false;
    }
  };

  protected void doPushFilterIntoJsonGroupScan(RelOptRuleCall call,
      FilterPrel filter, final ProjectPrel project, ScanPrel scan,
      JsonTableGroupScan groupScan, RexNode condition) {

    if (groupScan.isDisablePushdown() // Do not pushdown filter if it is disabled in plugin configuration
        || groupScan.isFilterPushedDown()) { // see below
      /*
       * The rule can get triggered again due to the transformed "scan => filter" sequence
       * created by the earlier execution of this rule when we could not do a complete
       * conversion of Optiq Filter's condition to HBase Filter. In such cases, we rely upon
       * this flag to not do a re-processing of the rule on the already transformed call.
       */
      return;
    }

    LogicalExpression conditionExp;
    try {
      conditionExp = DrillOptiq.toDrill(new DrillParseContext(PrelUtil.getPlannerSettings(call.getPlanner())), scan, condition);
    } catch (ClassCastException e) {
      // MD-771 bug in DrillOptiq.toDrill() causes filter condition on ITEM operator to throw ClassCastException
      // For such cases, we return without pushdown
      return;
    }
    final JsonConditionBuilder jsonConditionBuilder = new JsonConditionBuilder(groupScan, conditionExp);
    final JsonScanSpec newScanSpec = jsonConditionBuilder.parseTree();
    if (newScanSpec == null) {
      return; // no filter pushdown ==> No transformation.
    }

    final JsonTableGroupScan newGroupsScan = (JsonTableGroupScan) groupScan.clone(newScanSpec);
    newGroupsScan.setFilterPushedDown(true);

    final ScanPrel newScanPrel = new ScanPrel(scan.getCluster(), filter.getTraitSet(), newGroupsScan, scan.getRowType(), scan.getTable());

    // Depending on whether is a project in the middle, assign either scan or copy of project to childRel.
    final RelNode childRel = project == null ? newScanPrel : project.copy(project.getTraitSet(), ImmutableList.of((RelNode)newScanPrel));

    if (jsonConditionBuilder.isAllExpressionsConverted()) {
        /*
         * Since we could convert the entire filter condition expression into an HBase filter,
         * we can eliminate the filter operator altogether.
         */
      call.transformTo(childRel);
    } else {
      call.transformTo(filter.copy(filter.getTraitSet(), ImmutableList.of(childRel)));
    }
  }

  protected void doPushFilterIntoBinaryGroupScan(final RelOptRuleCall call,
                                                 final FilterPrel filter,
                                                 final ProjectPrel project,
                                                 final ScanPrel scan,
                                                 final BinaryTableGroupScan groupScan,
                                                 final RexNode condition) {

    if (groupScan.isFilterPushedDown()) {
      /*
       * The rule can get triggered again due to the transformed "scan => filter" sequence
       * created by the earlier execution of this rule when we could not do a complete
       * conversion of Optiq Filter's condition to HBase Filter. In such cases, we rely upon
       * this flag to not do a re-processing of the rule on the already transformed call.
       */
      return;
    }

    final LogicalExpression conditionExp = DrillOptiq.toDrill(new DrillParseContext(PrelUtil.getPlannerSettings(call.getPlanner())), scan, condition);
    final MapRDBFilterBuilder maprdbFilterBuilder = new MapRDBFilterBuilder(groupScan, conditionExp);
    final HBaseScanSpec newScanSpec = maprdbFilterBuilder.parseTree();
    if (newScanSpec == null) {
      return; //no filter pushdown ==> No transformation.
    }

    // Pass tableStats from old groupScan so we do not go and fetch stats (an expensive operation) again from MapR DB client.
    final BinaryTableGroupScan newGroupsScan =
        new BinaryTableGroupScan(groupScan.getUserName(), groupScan.getStoragePlugin(),
            groupScan.getFormatPlugin(), newScanSpec, groupScan.getColumns(),
            groupScan.getTableStats(), groupScan.getMetadataProvider());
    newGroupsScan.setFilterPushedDown(true);

    final ScanPrel newScanPrel = new ScanPrel(scan.getCluster(), filter.getTraitSet(), newGroupsScan, scan.getRowType(), scan.getTable());

    // Depending on whether is a project in the middle, assign either scan or copy of project to childRel.
    final RelNode childRel = project == null ? newScanPrel : project.copy(project.getTraitSet(), ImmutableList.of((RelNode)newScanPrel));

    if (maprdbFilterBuilder.isAllExpressionsConverted()) {
        /*
         * Since we could convert the entire filter condition expression into an HBase filter,
         * we can eliminate the filter operator altogether.
         */
      call.transformTo(childRel);
    } else {
      call.transformTo(filter.copy(filter.getTraitSet(), ImmutableList.of(childRel)));
    }
  }

}
