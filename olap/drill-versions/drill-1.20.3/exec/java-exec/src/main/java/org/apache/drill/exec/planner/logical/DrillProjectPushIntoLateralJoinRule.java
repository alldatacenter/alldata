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

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.tools.RelBuilderFactory;
import org.apache.drill.exec.planner.StarColumnHelper;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrillProjectPushIntoLateralJoinRule extends RelOptRule {

  public static final DrillProjectPushIntoLateralJoinRule INSTANCE =
    new DrillProjectPushIntoLateralJoinRule(RelFactories.LOGICAL_BUILDER);


  public DrillProjectPushIntoLateralJoinRule(RelBuilderFactory relFactory) {
    super(operand(DrillProjectRel.class,
        operand(DrillLateralJoinRel.class, any())),
      relFactory, null);
  }

  public void onMatch(RelOptRuleCall call) {
    DrillProjectRel origProj = call.rel(0);
    final DrillLateralJoinRel corr = call.rel(1);

    if (StarColumnHelper.containsStarColumn(origProj.getRowType()) ||
        StarColumnHelper.containsStarColumn(corr.getRowType()) ||
         corr.excludeCorrelateColumn) {
      return;
    }
    DrillRelOptUtil.InputRefVisitor collectRefs = new DrillRelOptUtil.InputRefVisitor();
    for (RexNode exp: origProj.getChildExps()) {
      exp.accept(collectRefs);
    }

    int correlationIndex = corr.getRequiredColumns().nextSetBit(0);
    for (RexInputRef inputRef : collectRefs.getInputRefs()) {
      if (inputRef.getIndex() == correlationIndex) {
        return;
      }
    }

    final RelNode left = corr.getLeft();
    final RelNode right = corr.getRight();
    final RelNode convertedLeft = convert(left, left.getTraitSet().plus(DrillRel.DRILL_LOGICAL).simplify());
    final RelNode convertedRight = convert(right, right.getTraitSet().plus(DrillRel.DRILL_LOGICAL).simplify());

    final RelTraitSet traits = corr.getTraitSet().plus(DrillRel.DRILL_LOGICAL);
    boolean trivial = DrillRelOptUtil.isTrivialProject(origProj, true);
    RelNode relNode = new DrillLateralJoinRel(corr.getCluster(),
                            traits, convertedLeft, convertedRight, true, corr.getCorrelationId(),
                            corr.getRequiredColumns(), corr.getJoinType());

    if (!trivial) {
      Map<Integer, Integer> mapWithoutCorr = buildMapWithoutCorrColumn(corr, correlationIndex);
      List<RexNode> outputExprs = DrillRelOptUtil.transformExprs(origProj.getCluster().getRexBuilder(), origProj.getChildExps(), mapWithoutCorr);

      relNode = new DrillProjectRel(origProj.getCluster(),
                                    left.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
                                    relNode, outputExprs, origProj.getRowType());
    }
    call.transformTo(relNode);
  }

  private Map<Integer, Integer> buildMapWithoutCorrColumn(RelNode corr, int correlationIndex) {
    int index = 0;
    Map<Integer, Integer> result = new HashMap();
    for (int i=0;i<corr.getRowType().getFieldList().size();i++) {
      if (i == correlationIndex) {
        continue;
      } else {
        result.put(i, index++);
      }
    }
    return result;
  }
}