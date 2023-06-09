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

import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.MetadataHandlerRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;

public class MetadataHandlerPrule extends Prule {
  public static final MetadataHandlerPrule INSTANCE = new MetadataHandlerPrule();

  public MetadataHandlerPrule() {
    super(RelOptHelper.any(MetadataHandlerRel.class, DrillRel.DRILL_LOGICAL),
        "MetadataHandlerPrule");
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    MetadataHandlerRel relNode = call.rel(0);
    RelNode input = relNode.getInput();
    RelTraitSet traits = input.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(DrillDistributionTrait.SINGLETON);
    RelNode convertedInput = convert(input, traits);
    call.transformTo(new MetadataHandlerPrel(relNode.getCluster(), traits,
        convertedInput,
        relNode.getContext()));
  }
}
