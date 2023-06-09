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
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.logical.DrillUnnestRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;

public class UnnestPrule extends Prule {
  public static final RelOptRule INSTANCE = new UnnestPrule();

  private UnnestPrule() {
    super(RelOptHelper.any(DrillUnnestRel.class), "UnnestPrule");
  }
  @Override
  public void onMatch(RelOptRuleCall call) {
    final DrillUnnestRel unnest = call.rel(0);
    RexNode ref = unnest.getRef();

    UnnestPrel unnestPrel = new UnnestPrel(unnest.getCluster(),
        unnest.getTraitSet().plus(Prel.DRILL_PHYSICAL), unnest.getRowType(), ref);

    call.transformTo(unnestPrel);
  }
}
