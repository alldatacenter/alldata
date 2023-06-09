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
package org.apache.drill.exec.store.jdbc;

import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillRelFactories;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.store.enumerable.plan.VertexDrel;

class JdbcIntermediatePrelConverterRule extends RelOptRule {

  private final RelTrait inTrait;
  private final RelTrait outTrait;

  public JdbcIntermediatePrelConverterRule(JdbcConvention jdbcConvention) {
    super(
        RelOptHelper.some(VertexDrel.class, DrillRel.DRILL_LOGICAL,
            RelOptHelper.any(RelNode.class, jdbcConvention)),
        DrillRelFactories.LOGICAL_BUILDER, "JDBC_PREL_Converter" + jdbcConvention);

    this.inTrait = DrillRel.DRILL_LOGICAL;
    this.outTrait = Prel.DRILL_PHYSICAL;
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    VertexDrel in = call.rel(0);
    RelNode jdbcIntermediatePrel = new JdbcIntermediatePrel(
        in.getCluster(),
        in.getTraitSet().replace(outTrait).plus(DrillDistributionTrait.SINGLETON),
        in.getInput(0));
    call.transformTo(jdbcIntermediatePrel);
  }

  @Override
  public boolean matches(RelOptRuleCall call) {
    return super.matches(call) && call.rel(0).getTraitSet().contains(inTrait);
  }
}
