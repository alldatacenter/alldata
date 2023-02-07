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

import java.io.IOException;
import java.util.List;

import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.RangePartitionExchange;
import org.apache.drill.exec.planner.cost.DrillCostBase.DrillCostFactory;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionField;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.linq4j.Ord;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;

import static org.apache.drill.exec.planner.index.Statistics.ROWCOUNT_UNKNOWN;

/**
 * A RangePartitionExchange provides the ability to divide up the rows into separate ranges or 'buckets'
 * based on the values of a set of columns (the range partitioning columns).
 */
public class RangePartitionExchangePrel extends ExchangePrel {

  /**
   * List of fields on which the range distribution should be done (typically this is 1 field)
   */
  private final List<DistributionField> fields;

  /**
   * The partitioning function to be used for doing the range partitioning
   */
  private final PartitionFunction partitionFunction;

  public RangePartitionExchangePrel(RelOptCluster cluster, RelTraitSet traitSet, RelNode input,
      List<DistributionField> fields, PartitionFunction partitionFunction) {
    super(cluster, traitSet, input);
    this.fields = fields;
    this.partitionFunction = partitionFunction;
    assert input.getConvention() == Prel.DRILL_PHYSICAL;
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    if (PrelUtil.getSettings(getCluster()).useDefaultCosting()) {
      return super.computeSelfCost(planner).multiplyBy(.1);
    }
    RelNode child = this.getInput();
    double inputRows = (mq == null)? ROWCOUNT_UNKNOWN : mq.getRowCount(child);

    // int  rowWidth = child.getRowType().getFieldCount() * DrillCostBase.AVG_FIELD_WIDTH;

    /* NOTE: the Exchange costing in general has to be examined in a broader context. A RangePartitionExchange
     * may be added for index plans with RowJeyJoin and Calcite compares the cost of this sub-plan with a
     * full table scan (FTS) sub-plan without an exchange.  The RelSubSet would have Filter-Project-TableScan for
     * the FTS and a RowKeyJoin whose right input is a RangePartitionExchange-IndexScan. Although a final UnionExchange
     * is done for both plans, the intermediate costing of index plan with exchange makes it more expensive than the FTS
     * sub-plan, even though the final cost of the overall FTS would have been more expensive.
     */

    // commenting out following based on comments above
    // double rangePartitionCpuCost = DrillCostBase.RANGE_PARTITION_CPU_COST * inputRows;
    // double svrCpuCost = DrillCostBase.SVR_CPU_COST * inputRows;
    // double networkCost = DrillCostBase.BYTE_NETWORK_COST * inputRows * rowWidth;
    DrillCostFactory costFactory = (DrillCostFactory)planner.getCostFactory();
    return costFactory.makeCost(inputRows, 0, 0, 0 /* see comments above */);
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new RangePartitionExchangePrel(getCluster(), traitSet, sole(inputs), fields, partitionFunction);
  }

  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    Prel child = (Prel) this.getInput();

    PhysicalOperator childPOP = child.getPhysicalOperator(creator);

    if (PrelUtil.getSettings(getCluster()).isSingleMode()) {
      return childPOP;
    }

    RangePartitionExchange g = new RangePartitionExchange(childPOP, partitionFunction);
    return creator.addMetadata(this, g);
  }

  public List<DistributionField> getFields() {
    return this.fields;
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    super.explainTerms(pw);
      for (Ord<DistributionField> ord : Ord.zip(fields)) {
        pw.item("dist" + ord.i, ord.e);
      }
    return pw;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }

  @Override
  public SelectionVectorMode[] getSupportedEncodings() {
    return SelectionVectorMode.ALL;
  }

}
