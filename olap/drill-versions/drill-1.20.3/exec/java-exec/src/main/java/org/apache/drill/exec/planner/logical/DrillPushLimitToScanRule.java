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
package org.apache.drill.exec.planner.logical;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;

public abstract class DrillPushLimitToScanRule extends RelOptRule {
  private static final Logger logger = LoggerFactory.getLogger(DrillPushLimitToScanRule.class);

  private DrillPushLimitToScanRule(RelOptRuleOperand operand, String description) {
    super(operand, DrillRelFactories.LOGICAL_BUILDER, description);
  }

  public static DrillPushLimitToScanRule LIMIT_ON_SCAN =
      new DrillPushLimitToScanRule(RelOptHelper.some(DrillLimitRel.class, RelOptHelper.any(DrillScanRel.class)),
          "DrillPushLimitToScanRule_LimitOnScan") {
    @Override
    public boolean matches(RelOptRuleCall call) {
      DrillLimitRel limitRel = call.rel(0);
      DrillScanRel scanRel = call.rel(1);
      // For now only applies to Parquet. And pushdown only applies to LIMIT but not OFFSET,
      // so if getFetch() return null no need to run this rule.
      if (scanRel.getGroupScan().supportsLimitPushdown() && (limitRel.getFetch() != null)) {
        return true;
      }
      return false;
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
        DrillLimitRel limitRel = call.rel(0);
        DrillScanRel scanRel = call.rel(1);
        doOnMatch(call, limitRel, scanRel, null);
    }
  };

  public static DrillPushLimitToScanRule LIMIT_ON_PROJECT = new DrillPushLimitToScanRule(
      RelOptHelper.some(DrillLimitRel.class, RelOptHelper.any(DrillProjectRel.class)), "DrillPushLimitToScanRule_LimitOnProject") {
    @Override
    public boolean matches(RelOptRuleCall call) {
      DrillLimitRel limitRel = call.rel(0);
      DrillProjectRel projectRel = call.rel(1);
      // pushdown only apply limit but not offset,
      // so if getFetch() return null no need to run this rule.
      // Do not push across Project containing CONVERT_FROMJSON for limit 0 queries. For limit 0 queries, this would
      // mess up the schema since Convert_FromJson() is different from other regular functions in that it only knows
      // the output schema after evaluation is performed. When input has 0 row, Drill essentially does not have a way
      // to know the output type.
      // Cannot pushdown limit and offset in to flatten as long as we don't know data distribution in flattened field
      if (!limitRel.isPushDown() && (limitRel.getFetch() != null)
          && (!DrillRelOptUtil.isLimit0(limitRel.getFetch())
            || !DrillRelOptUtil.isProjectOutputSchemaUnknown(projectRel))
          && !DrillRelOptUtil.isProjectOutputRowcountUnknown(projectRel)) {
        return true;
      }
      return false;
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
      DrillLimitRel limitRel = call.rel(0);
      DrillProjectRel projectRel = call.rel(1);
      RelNode child = projectRel.getInput();
      final RelNode limitUnderProject = limitRel.copy(limitRel.getTraitSet(), ImmutableList.of(child));
      final RelNode newProject = projectRel.copy(projectRel.getTraitSet(), ImmutableList.of(limitUnderProject));
      call.transformTo(newProject);
    }
  };

  protected void doOnMatch(RelOptRuleCall call, DrillLimitRel limitRel,
      DrillScanRel scanRel, DrillProjectRel projectRel) {
    try {
      final int rowCountRequested = (int) limitRel.estimateRowCount(limitRel.getCluster().getMetadataQuery());

      final GroupScan newGroupScan = scanRel.getGroupScan().applyLimit(rowCountRequested);

      if (newGroupScan == null ) {
        return;
      }

      DrillScanRel newScanRel = new DrillScanRel(scanRel.getCluster(),
          scanRel.getTraitSet(),
          scanRel.getTable(),
          newGroupScan,
          scanRel.getRowType(),
          scanRel.getColumns(),
          scanRel.partitionFilterPushdown());

      final RelNode newLimit;
      if (projectRel != null) {
        final RelNode newProject = projectRel.copy(projectRel.getTraitSet(), ImmutableList.of(newScanRel));
        newLimit = limitRel.copy(limitRel.getTraitSet(), ImmutableList.of(newProject));
      } else {
        newLimit = limitRel.copy(limitRel.getTraitSet(), ImmutableList.of(newScanRel));
      }

      call.transformTo(newLimit);
      logger.debug("Converted to a new DrillScanRel" + newScanRel.getGroupScan());
    }  catch (Exception e) {
      logger.warn("Exception while using the pruned partitions.", e);
    }
  }
}
