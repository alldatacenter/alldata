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
import org.apache.drill.exec.planner.logical.MetadataControllerRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;

public class MetadataControllerPrule extends Prule {
  public static final MetadataControllerPrule INSTANCE = new MetadataControllerPrule();

  private MetadataControllerPrule() {
    super(RelOptHelper.some(MetadataControllerRel.class, DrillRel.DRILL_LOGICAL,
        RelOptHelper.any(RelNode.class)), "MetadataControllerPrule");
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    MetadataControllerRel relNode = call.rel(0);
    RelNode left = relNode.getLeft();
    RelNode right = relNode.getRight();
    RelTraitSet traits = call.getPlanner().emptyTraitSet().plus(Prel.DRILL_PHYSICAL).plus(DrillDistributionTrait.SINGLETON);
    RelNode convertedLeft = convert(left, traits);
    RelNode convertedRight = convert(right, traits);
    call.transformTo(new MetadataControllerPrel(relNode.getCluster(),
      // force singleton execution since this is the final step for metadata collection which collects all results into one
      relNode.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(DrillDistributionTrait.SINGLETON),
      convertedLeft, convertedRight, relNode.getContext()));
  }
}
