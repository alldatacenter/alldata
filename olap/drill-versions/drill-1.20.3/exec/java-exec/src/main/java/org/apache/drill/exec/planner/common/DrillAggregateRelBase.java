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
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.planner.cost.DrillCostBase.DrillCostFactory;
import org.apache.drill.exec.planner.physical.PrelUtil;

import java.util.List;

/**
 * Base class for logical and physical Aggregations implemented in Drill
 */
public abstract class DrillAggregateRelBase extends Aggregate implements DrillRelNode {

  public DrillAggregateRelBase(RelOptCluster cluster, RelTraitSet traits, RelNode child,
                               ImmutableBitSet groupSet, List<ImmutableBitSet> groupSets, List<AggregateCall> aggCalls) {
    super(cluster, traits, child, groupSet, groupSets, aggCalls);
  }

  /**
   * Estimate cost of hash agg. Called by DrillAggregateRel.computeSelfCost() and HashAggPrel.computeSelfCost()
  */
  protected RelOptCost computeHashAggCost(RelOptPlanner planner, RelMetadataQuery mq) {
    if(PrelUtil.getSettings(getCluster()).useDefaultCosting()) {
      return super.computeSelfCost(planner, mq).multiplyBy(.1);
    }
    RelNode child = this.getInput();
    double inputRows = mq.getRowCount(child);

    int numGroupByFields = this.getGroupCount();
    int numAggrFields = this.aggCalls.size();
    // cpu cost of hashing each grouping key
    double cpuCost = DrillCostBase.HASH_CPU_COST * numGroupByFields * inputRows;
    // add cpu cost for computing the aggregate functions
    cpuCost += DrillCostBase.FUNC_CPU_COST * numAggrFields * inputRows;
    double diskIOCost = 0; // assume in-memory for now until we enforce operator-level memory constraints

    // TODO: use distinct row count
    // + hash table template stuff
    double factor = PrelUtil.getPlannerSettings(planner).getOptions()
        .getOption(ExecConstants.HASH_AGG_TABLE_FACTOR_KEY).float_val;
    long fieldWidth = PrelUtil.getPlannerSettings(planner).getOptions()
        .getOption(ExecConstants.AVERAGE_FIELD_WIDTH_KEY).num_val;

    // table + hashValues + links
    double memCost =
        (
            (fieldWidth * numGroupByFields) +
                IntHolder.WIDTH +
                IntHolder.WIDTH
        ) * inputRows * factor;

    DrillCostFactory costFactory = (DrillCostFactory) planner.getCostFactory();
    return costFactory.makeCost(inputRows, cpuCost, diskIOCost, 0 /* network cost */, memCost);

  }

  protected RelOptCost computeLogicalAggCost(RelOptPlanner planner, RelMetadataQuery mq) {
    // Similar to Join cost estimation, use HashAgg cost during the logical planning.
    return computeHashAggCost(planner, mq);
  }

  @Override
  public double estimateRowCount(RelMetadataQuery mq) {
    // Get the number of distinct group-by key values present in the input
    if (!DrillRelOptUtil.guessRows(this)) {
      return mq.getRowCount(this);
    } else {
      return super.estimateRowCount(mq);
    }
  }
}
