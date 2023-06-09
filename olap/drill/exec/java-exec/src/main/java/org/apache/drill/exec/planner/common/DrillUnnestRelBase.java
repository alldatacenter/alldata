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
package org.apache.drill.exec.planner.common;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.AbstractRelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.cost.DrillCostBase;

public class DrillUnnestRelBase extends AbstractRelNode implements DrillRelNode {

  final protected RexNode ref;
  final public static String IMPLICIT_COLUMN = DrillRelOptUtil.IMPLICIT_COLUMN;

  public DrillUnnestRelBase(RelOptCluster cluster, RelTraitSet traitSet, RexNode ref) {
    super(cluster, traitSet);
    this.ref = ref;
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {

    double rowCount = mq.getRowCount(this);
    // Attribute small cost for projecting simple fields. In reality projecting simple columns in not free and
    // this allows projection pushdown/project-merge rules to kick-in thereby eliminating unneeded columns from
    // the projection.
    double cpuCost = DrillCostBase.BASE_CPU_COST * rowCount * this.getRowType().getFieldCount();

    DrillCostBase.DrillCostFactory costFactory = (DrillCostBase.DrillCostFactory) planner.getCostFactory();
    return costFactory.makeCost(rowCount, cpuCost, 0, 0);
  }

  public RexNode getRef() {
    return this.ref;
  }
}