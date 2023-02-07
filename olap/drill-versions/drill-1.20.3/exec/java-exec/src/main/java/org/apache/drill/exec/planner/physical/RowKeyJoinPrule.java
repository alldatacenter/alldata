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

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.exec.planner.logical.RowKeyJoinRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;

public class RowKeyJoinPrule extends JoinPruleBase {

  public static final RelOptRule INSTANCE = new RowKeyJoinPrule("Prel.RowKeyJoinPrule",
      RelOptHelper.any(RowKeyJoinRel.class));
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RowKeyJoinPrule.class);

  private RowKeyJoinPrule(String name, RelOptRuleOperand operand) {
    super(operand, name);
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    PlannerSettings settings = PrelUtil.getPlannerSettings(call.getPlanner());

    final RowKeyJoinRel join = call.rel(0);
    final RelNode left = join.getLeft();
    final RelNode right = join.getRight();

    if (!checkPreconditions(join, left, right, settings)) {
      return;
    }

    try {
      if (!settings.isRowKeyJoinConversionUsingHashJoin()) {
        // For now, lets assume rowkey join does not preserve collation
        createRangePartitionRightPlan(call, join, PhysicalJoinType.HASH_JOIN, true,
            left, right, null /* left collation */, null /* right collation */);
      } else {
        createRangePartitionRightPlan(call, join, PhysicalJoinType.HASH_JOIN, false,
            left, right, null /* left collation */, null /* right collation */);
      }
    } catch (Exception e) {
      logger.warn(e.toString());
    }
  }
}
