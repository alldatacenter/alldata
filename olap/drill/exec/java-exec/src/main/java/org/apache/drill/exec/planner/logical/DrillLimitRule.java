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

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Sort;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.logical.LogicalSort;

/**
 * This rule converts a Sort that has either a offset and fetch into a Drill Sort and LimitPOP Rel
 */
public class DrillLimitRule extends RelOptRule {
  public static DrillLimitRule INSTANCE = new DrillLimitRule();

  private DrillLimitRule() {
    super(RelOptHelper.any(LogicalSort.class, Convention.NONE),
        DrillRelFactories.LOGICAL_BUILDER, "DrillLimitRule");
  }

  @Override
  public boolean matches(RelOptRuleCall call) {
    final Sort sort = call.rel(0);
    return sort.offset != null || sort.fetch != null;
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final Sort incomingSort = call.rel(0);
    final RelTraitSet incomingTraits = incomingSort.getTraitSet();
    RelNode input = incomingSort.getInput();

    // if the Optiq sort rel includes a collation and a limit, we need to create a copy the sort rel that excludes the
    // limit information.
    if (!incomingSort.getCollation().getFieldCollations().isEmpty()) {
      input = incomingSort.copy(incomingTraits, input, incomingSort.getCollation(), null, null);
    }

    RelNode convertedInput = convert(input, input.getTraitSet().plus(DrillRel.DRILL_LOGICAL).simplify());
    call.transformTo(new DrillLimitRel(
        incomingSort.getCluster(), convertedInput.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
        convertedInput, incomingSort.offset, incomingSort.fetch));
  }

}
