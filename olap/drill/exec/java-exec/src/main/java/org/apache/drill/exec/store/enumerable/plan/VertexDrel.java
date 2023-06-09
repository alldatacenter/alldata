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

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.SingleRel;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.exec.planner.logical.DrillImplementor;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.util.Utilities;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static org.apache.drill.exec.planner.logical.DrillScanRel.STAR_COLUMN_COST;

/**
 * The vertex simply holds the child nodes but contains its own traits.
 * Used for completing Drill logical planning when child nodes have some specific traits.
 */
public class VertexDrel extends SingleRel implements DrillRel {

  public VertexDrel(RelOptCluster cluster, RelTraitSet traits, RelNode input) {
    super(cluster, traits, input);
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new VertexDrel(getCluster(), traitSet, inputs.iterator().next());
  }

  @Override
  protected Object clone() {
    return copy(getTraitSet(), getInputs());
  }

  @Override
  public LogicalOperator implement(DrillImplementor implementor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    double rowCount = estimateRowCount(mq);
    double columnCount = Utilities.isStarQuery(getRowType()) ? STAR_COLUMN_COST : getRowType().getFieldCount();
    double valueCount = rowCount * columnCount;
    // columns count is considered during cost calculation to make preferable plans
    // with pushed plugin project operators since in the opposite case planner wouldn't consider
    // a plan with additional plugin projection that reduces columns as better than a plan without it
    return planner.getCostFactory().makeCost(rowCount, valueCount, 0).multiplyBy(0.1);
  }
}
