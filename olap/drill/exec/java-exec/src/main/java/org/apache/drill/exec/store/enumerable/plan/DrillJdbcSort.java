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
package org.apache.drill.exec.store.enumerable.plan;

import org.apache.calcite.adapter.jdbc.JdbcRules;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.exec.planner.cost.DrillCostBase;

public class DrillJdbcSort extends JdbcRules.JdbcSort {
  public DrillJdbcSort(RelOptCluster cluster, RelTraitSet traitSet, RelNode input, RelCollation collation, RexNode offset, RexNode fetch) {
    super(cluster, traitSet, input, collation, offset, fetch);
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    if (collation.getFieldCollations().isEmpty()) {
      // The case when sort node represents a regular limit without actual sort.
      // Drill separates limit from sort, and they have different formulas for cost calculation.
      // The cost for the case of the JDBC limit operator should correspond to the cost of the Drill's one.
      double numRows = mq.getRowCount(this);
      double cpuCost = DrillCostBase.COMPARE_CPU_COST * numRows;
      DrillCostBase.DrillCostFactory costFactory = (DrillCostBase.DrillCostFactory) planner.getCostFactory();
      // adjust cost to handle the case when the original limit was split
      return costFactory.makeCost(numRows, cpuCost, 0, 0).multiplyBy(0.1);
    }
    return super.computeSelfCost(planner, mq);
  }
}
