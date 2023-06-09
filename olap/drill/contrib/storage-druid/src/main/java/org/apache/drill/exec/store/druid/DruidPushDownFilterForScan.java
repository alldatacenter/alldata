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
package org.apache.drill.exec.store.druid;

import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.planner.logical.DrillOptiq;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.FilterPrel;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

public class DruidPushDownFilterForScan extends StoragePluginOptimizerRule {

  public static final StoragePluginOptimizerRule INSTANCE = new DruidPushDownFilterForScan();

  private DruidPushDownFilterForScan() {
    super(
      RelOptHelper.some(FilterPrel.class, RelOptHelper.any(ScanPrel.class)),
      "DruidPushDownFilterForScan");
  }

  @Override
  public void onMatch(RelOptRuleCall relOptRuleCall) {
    final ScanPrel scan = relOptRuleCall.rel(1);
    final FilterPrel filter = relOptRuleCall.rel(0);
    final RexNode condition = filter.getCondition();

    DruidGroupScan groupScan = (DruidGroupScan) scan.getGroupScan();
    if (groupScan.isFilterPushedDown()) {
      return;
    }

    LogicalExpression conditionExp =
      DrillOptiq.toDrill(
        new DrillParseContext(PrelUtil.getPlannerSettings(relOptRuleCall.getPlanner())),
        scan,
        condition);

    DruidFilterBuilder druidFilterBuilder =
      new DruidFilterBuilder(groupScan, conditionExp);

    DruidScanSpec newScanSpec = druidFilterBuilder.parseTree();
    if (newScanSpec == null) {
      return; // no filter pushdown so nothing to apply.
    }

    DruidGroupScan newGroupsScan =
        new DruidGroupScan(
            groupScan.getUserName(),
            groupScan.getStoragePlugin(),
            newScanSpec,
            groupScan.getColumns(),
            groupScan.getMaxRecordsToRead());
    newGroupsScan.setFilterPushedDown(true);

    ScanPrel newScanPrel = scan.copy(filter.getTraitSet(), newGroupsScan, filter.getRowType());
    if (druidFilterBuilder.isAllExpressionsConverted()) {
      /*
       * Since we could convert the entire filter condition expression into a
       * Druid filter, we can eliminate the filter operator altogether.
       */
      relOptRuleCall.transformTo(newScanPrel);
    } else {
      relOptRuleCall.transformTo(filter.copy(filter.getTraitSet(),
        ImmutableList.of(newScanPrel)));
    }
  }

  @Override
  public boolean matches(RelOptRuleCall call) {
    final ScanPrel scan = call.rel(1);
    if (scan.getGroupScan() instanceof DruidGroupScan) {
      return super.matches(call);
    }
    return false;
  }
}
