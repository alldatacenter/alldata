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


import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.rel.core.Correlate;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rel.rules.ProjectCorrelateTransposeRule;
import org.apache.calcite.rel.rules.PushProjector;
import org.apache.calcite.tools.RelBuilderFactory;

public class DrillProjectLateralJoinTransposeRule extends ProjectCorrelateTransposeRule {

  public static final DrillProjectLateralJoinTransposeRule INSTANCE = new DrillProjectLateralJoinTransposeRule(PushProjector.ExprCondition.TRUE, RelFactories.LOGICAL_BUILDER);

  public DrillProjectLateralJoinTransposeRule(PushProjector.ExprCondition preserveExprCondition, RelBuilderFactory relFactory) {
    super(preserveExprCondition, relFactory);
  }

  @Override
  public boolean matches(RelOptRuleCall call) {
    Correlate correlate = call.rel(1);


    // No need to call ProjectCorrelateTransposeRule if the current lateralJoin contains excludeCorrelationColumn set to true.
    // This is needed as the project push into Lateral join rule changes the output row type which will fail assertions in ProjectCorrelateTransposeRule.
    if (correlate instanceof DrillLateralJoinRel &&
        ((DrillLateralJoinRel)correlate).excludeCorrelateColumn) {
      return false;
    }

    return true;
  }
}