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

import java.util.List;

import org.apache.calcite.rel.RelCollations;
import org.apache.drill.exec.planner.logical.DrillJoinRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.util.trace.CalciteTrace;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;

public class MergeJoinPrule extends JoinPruleBase {
  public static final RelOptRule DIST_INSTANCE = new MergeJoinPrule("Prel.MergeJoinDistPrule", RelOptHelper.any(DrillJoinRel.class), true);
  public static final RelOptRule BROADCAST_INSTANCE = new MergeJoinPrule("Prel.MergeJoinBroadcastPrule", RelOptHelper.any(DrillJoinRel.class), false);

  protected static final Logger tracer = CalciteTrace.getPlannerTracer();

  private final boolean isDist;

  private MergeJoinPrule(String name, RelOptRuleOperand operand, boolean isDist) {
    super(operand, name);
    this.isDist = isDist;
  }

  @Override
  public boolean matches(RelOptRuleCall call) {
    return PrelUtil.getPlannerSettings(call.getPlanner()).isMergeJoinEnabled();
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    PlannerSettings settings = PrelUtil.getPlannerSettings(call.getPlanner());
    final DrillJoinRel join = call.rel(0);
    final RelNode left = join.getLeft();
    final RelNode right = join.getRight();

    if (!checkPreconditions(join, left, right, settings)) {
      return;
    }

    boolean hashSingleKey = PrelUtil.getPlannerSettings(call.getPlanner()).isHashSingleKey();

    try {
      RelCollation collationLeft = getCollation(join.getLeftKeys());
      RelCollation collationRight = getCollation(join.getRightKeys());

      if(isDist){
        createDistBothPlan(call, join, PhysicalJoinType.MERGE_JOIN, left, right, collationLeft, collationRight, hashSingleKey);
      }else{
        if (checkBroadcastConditions(call.getPlanner(), join, left, right)) {
          createBroadcastPlan(call, join, join.getCondition(), PhysicalJoinType.MERGE_JOIN,
              left, right, collationLeft, collationRight);
        }
      }

    } catch (InvalidRelException e) {
      tracer.warn(e.toString());
    }
  }

  private RelCollation getCollation(List<Integer> keys) {
    List<RelFieldCollation> fields = Lists.newArrayList();
    for (int key : keys) {
      fields.add(new RelFieldCollation(key));
    }
    return RelCollations.of(fields);
  }

}
