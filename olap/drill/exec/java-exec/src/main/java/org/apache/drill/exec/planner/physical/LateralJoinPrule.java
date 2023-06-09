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
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.exec.planner.logical.DrillLateralJoinRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;

public class LateralJoinPrule extends Prule {
  public static final RelOptRule INSTANCE = new LateralJoinPrule("Prel.LateralJoinPrule",
      RelOptHelper.any(DrillLateralJoinRel.class));

  private LateralJoinPrule(String name, RelOptRuleOperand operand) {
    super(operand, name);
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final DrillLateralJoinRel lateralJoinRel = call.rel(0);
    final RelNode left = lateralJoinRel.getLeft();
    final RelNode right = lateralJoinRel.getRight();
    RelTraitSet traitsLeft = left.getTraitSet().plus(Prel.DRILL_PHYSICAL);
    RelTraitSet traitsRight = right.getTraitSet().plus(Prel.DRILL_PHYSICAL);

    RelTraitSet corrTraits = traitsLeft.plus(DrillDistributionTrait.RANDOM_DISTRIBUTED);

    final RelNode convertedLeft = convert(left, traitsLeft);
    final RelNode convertedRight = convert(right, traitsRight);

    final LateralJoinPrel lateralJoinPrel = new LateralJoinPrel(lateralJoinRel.getCluster(),
                                  corrTraits,
                                  convertedLeft, convertedRight, lateralJoinRel.excludeCorrelateColumn, lateralJoinRel.getCorrelationId(),
                                  lateralJoinRel.getRequiredColumns(),lateralJoinRel.getJoinType());
    call.transformTo(lateralJoinPrel);
  }
}
