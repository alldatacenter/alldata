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

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalAggregate;

/**
 * Rule that converts an {@link LogicalAggregate} to a {@link DrillAggregateRel}, implemented by a Drill "segment" operation
 * followed by a "collapseaggregate" operation.
 */
public class DrillAggregateRule extends RelOptRule {
  public static final RelOptRule INSTANCE = new DrillAggregateRule();

  private DrillAggregateRule() {
    super(
        RelOptHelper.some(LogicalAggregate.class,
            Convention.NONE, RelOptHelper.any(RelNode.class)),
        DrillRelFactories.LOGICAL_BUILDER,
        "DrillAggregateRule");
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final LogicalAggregate aggregate = call.rel(0);
    final RelNode input = call.rel(1);

    if (aggregate.containsDistinctCall()) {
      // currently, don't use this rule if any of the aggregates contains DISTINCT
      return;
    }

    final RelTraitSet traits = aggregate.getTraitSet().plus(DrillRel.DRILL_LOGICAL);
    final RelNode convertedInput = convert(input, input.getTraitSet().plus(DrillRel.DRILL_LOGICAL).simplify());
    call.transformTo(new DrillAggregateRel(aggregate.getCluster(), traits, convertedInput,
        aggregate.getGroupSet(), aggregate.getGroupSets(), aggregate.getAggCallList()));
  }
}
