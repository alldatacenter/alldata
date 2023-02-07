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
import org.apache.drill.exec.physical.config.HashToMergeExchange;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.planner.cost.DrillCostBase.DrillCostFactory;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionField;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;

public class HashToMergeExchangePrel extends ExchangePrel {

  private final List<DistributionField> distFields;
  private int numEndPoints = 0;
  private final RelCollation collation;

  public HashToMergeExchangePrel(RelOptCluster cluster, RelTraitSet traitSet, RelNode input,
                                 List<DistributionField> fields,
                                 RelCollation collation,
                                 int numEndPoints) {
    super(cluster, traitSet, input);
    this.distFields = fields;
    this.collation = collation;
    this.numEndPoints = numEndPoints;
    assert input.getConvention() == Prel.DRILL_PHYSICAL;
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    if (PrelUtil.getSettings(getCluster()).useDefaultCosting()) {
      return super.computeSelfCost(planner, mq).multiplyBy(.1);
    }
    RelNode child = this.getInput();
    double inputRows = mq.getRowCount(child);

    int rowWidth = child.getRowType().getFieldCount() * DrillCostBase.AVG_FIELD_WIDTH;
    double hashCpuCost = DrillCostBase.HASH_CPU_COST * inputRows * distFields.size();
    double svrCpuCost = DrillCostBase.SVR_CPU_COST * inputRows;
    double mergeCpuCost = DrillCostBase.COMPARE_CPU_COST * inputRows * (Math.log(numEndPoints) / Math.log(2));
    double networkCost = DrillCostBase.BYTE_NETWORK_COST * inputRows * rowWidth;
    DrillCostFactory costFactory = (DrillCostFactory) planner.getCostFactory();
    return costFactory.makeCost(inputRows, hashCpuCost + svrCpuCost + mergeCpuCost, 0, networkCost);
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new HashToMergeExchangePrel(getCluster(), traitSet, sole(inputs), distFields,
        this.collation, numEndPoints);
  }

  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    Prel child = (Prel) this.getInput();

    PhysicalOperator childPOP = child.getPhysicalOperator(creator);

    if (PrelUtil.getSettings(getCluster()).isSingleMode()) {
      return childPOP;
    }

    HashToMergeExchange g = new HashToMergeExchange(childPOP,
        HashPrelUtil.getHashExpression(this.distFields, getInput().getRowType()),
        PrelUtil.getOrdering(this.collation, getInput().getRowType()));
    return creator.addMetadata(this, g);

  }

  public RelCollation getCollation() {
    return this.collation;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }

}
