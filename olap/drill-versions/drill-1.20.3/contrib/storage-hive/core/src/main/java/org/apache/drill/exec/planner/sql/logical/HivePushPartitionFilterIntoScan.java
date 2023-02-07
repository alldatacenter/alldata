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
package org.apache.drill.exec.planner.sql.logical;

import org.apache.calcite.rel.core.TableScan;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.PartitionDescriptor;
import org.apache.drill.exec.planner.logical.DrillFilterRel;
import org.apache.drill.exec.planner.logical.DrillProjectRel;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.logical.partition.PruneScanRule;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.sql.HivePartitionDescriptor;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.hive.HiveScan;
import org.apache.calcite.plan.RelOptRuleCall;

public abstract class HivePushPartitionFilterIntoScan {

  public static final StoragePluginOptimizerRule getFilterOnProject(OptimizerRulesContext optimizerRulesContext,
      final String defaultPartitionValue) {
    return new PruneScanRule(
        RelOptHelper.some(DrillFilterRel.class, RelOptHelper.some(DrillProjectRel.class, RelOptHelper.any(DrillScanRel.class))),
        "HivePushPartitionFilterIntoScan:Filter_On_Project_Hive",
        optimizerRulesContext) {

      @Override
      public PartitionDescriptor getPartitionDescriptor(PlannerSettings settings, TableScan scanRel) {
        return new HivePartitionDescriptor(settings, (DrillScanRel) scanRel, getOptimizerRulesContext().getManagedBuffer(),
            defaultPartitionValue);
      }

      @Override
      public boolean matches(RelOptRuleCall call) {
        final DrillScanRel scan = (DrillScanRel) call.rel(2);
        GroupScan groupScan = scan.getGroupScan();
        // this rule is applicable only for Hive based partition pruning
        if (PrelUtil.getPlannerSettings(scan.getCluster().getPlanner()).isHepPartitionPruningEnabled()) {
          return groupScan instanceof HiveScan && groupScan.supportsPartitionFilterPushdown() && !scan.partitionFilterPushdown();
        } else {
          return groupScan instanceof HiveScan && groupScan.supportsPartitionFilterPushdown();
        }
      }

      @Override
      public void onMatch(RelOptRuleCall call) {
        final DrillFilterRel filterRel =  call.rel(0);
        final DrillProjectRel projectRel = call.rel(1);
        final DrillScanRel scanRel = call.rel(2);
        doOnMatch(call, filterRel, projectRel, scanRel);
      }
    };
  }

  public static final StoragePluginOptimizerRule getFilterOnScan(OptimizerRulesContext optimizerRulesContext,
      final String defaultPartitionValue) {
    return new PruneScanRule(
        RelOptHelper.some(DrillFilterRel.class, RelOptHelper.any(DrillScanRel.class)),
        "HivePushPartitionFilterIntoScan:Filter_On_Scan_Hive", optimizerRulesContext) {

      @Override
      public PartitionDescriptor getPartitionDescriptor(PlannerSettings settings, TableScan scanRel) {
        return new HivePartitionDescriptor(settings, (DrillScanRel) scanRel, getOptimizerRulesContext().getManagedBuffer(),
            defaultPartitionValue);
      }

      @Override
      public boolean matches(RelOptRuleCall call) {
        final DrillScanRel scan = (DrillScanRel) call.rel(1);
        GroupScan groupScan = scan.getGroupScan();
        // this rule is applicable only for Hive based partition pruning
        if (PrelUtil.getPlannerSettings(scan.getCluster().getPlanner()).isHepPartitionPruningEnabled()) {
          return groupScan instanceof HiveScan && groupScan.supportsPartitionFilterPushdown() && !scan.partitionFilterPushdown();
        } else {
          return groupScan instanceof HiveScan && groupScan.supportsPartitionFilterPushdown();
        }
      }

      @Override
      public void onMatch(RelOptRuleCall call) {
        final DrillFilterRel filterRel = call.rel(0);
        final DrillScanRel scanRel = call.rel(1);
        doOnMatch(call, filterRel, null, scanRel);
      }
    };
  }
}
