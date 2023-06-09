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

import org.apache.drill.exec.planner.logical.DrillUnionRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.util.trace.CalciteTrace;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;

public class UnionDistinctPrule extends Prule {
  public static final RelOptRule INSTANCE = new UnionDistinctPrule();
  protected static final Logger tracer = CalciteTrace.getPlannerTracer();

  private UnionDistinctPrule() {
    super(
        RelOptHelper.any(DrillUnionRel.class), "Prel.UnionDistinctPrule");
  }

  @Override
  public boolean matches(RelOptRuleCall call) {
    DrillUnionRel union = call.rel(0);
    return (union.isDistinct() && union.isHomogeneous(false /* don't compare names */));
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final DrillUnionRel union = call.rel(0);
    final List<RelNode> inputs = union.getInputs();
    List<RelNode> convertedInputList = Lists.newArrayList();
    RelTraitSet traits = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL);

    try {
      for (RelNode input : inputs) {
        RelNode convertedInput = convert(input, PrelUtil.fixTraits(call, traits));
        convertedInputList.add(convertedInput);
      }

      traits = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL).plus(DrillDistributionTrait.SINGLETON);
      UnionDistinctPrel unionDistinct =
          new UnionDistinctPrel(union.getCluster(), traits, convertedInputList,
              false /* compatibility already checked during logical phase */);

      call.transformTo(unionDistinct);

    } catch (InvalidRelException e) {
      tracer.warn(e.toString());
    }
  }

}
