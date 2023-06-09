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
import java.util.Iterator;

import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.RowKeyJoinPOP;
import org.apache.drill.exec.planner.cost.DrillCostBase.DrillCostFactory;
import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class RowKeyJoinPrel extends JoinPrel implements Prel {

  double estimatedRowCount = -1;
  public RowKeyJoinPrel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right,
      RexNode condition, JoinRelType joinType) throws InvalidRelException {
    super(cluster, traits, left, right, condition, joinType);
    Preconditions.checkArgument(joinType == JoinRelType.INNER);
  }

  public RowKeyJoinPrel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right,
                        RexNode condition, JoinRelType joinType, boolean isSemiJoin) throws InvalidRelException {
    super(cluster, traits, left, right, condition, joinType, isSemiJoin);
    Preconditions.checkArgument(joinType == JoinRelType.INNER);
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    PhysicalOperator leftPop = ((Prel)left).getPhysicalOperator(creator);
    PhysicalOperator rightPop = ((Prel)right).getPhysicalOperator(creator);
    RowKeyJoinPOP rkPop = new RowKeyJoinPOP(leftPop, rightPop);
    return creator.addMetadata(this, rkPop);
  }

  @Override
  public double estimateRowCount(RelMetadataQuery mq) {
    if (estimatedRowCount >= 0) {
      return estimatedRowCount;
    }
    return this.getLeft().getRows();
  }

  @Override
  public Join copy(RelTraitSet traitSet, RexNode conditionExpr, RelNode left, RelNode right,
      JoinRelType joinType, boolean semiJoinDone) {
    try {
      RowKeyJoinPrel rkj = new RowKeyJoinPrel(this.getCluster(), traitSet, left, right, conditionExpr,
          joinType, isSemiJoin());
      rkj.setEstimatedRowCount(this.estimatedRowCount);
      return rkj;
    } catch (InvalidRelException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    if(PrelUtil.getSettings(getCluster()).useDefaultCosting()) {
      return super.computeSelfCost(planner).multiplyBy(.1);
    }
    double rowCount = mq.getRowCount(this.getRight());
    DrillCostFactory costFactory = (DrillCostFactory) planner.getCostFactory();

    // RowKeyJoin operator by itself incurs negligible CPU and I/O cost since it is not doing a real join.
    // The actual cost is attributed to the skip-scan (random I/O). The RK join will hold 1 batch in memory but
    // it is not making any extra copy of either the left or right batches, so the memory cost is 0
    return costFactory.makeCost(rowCount, 0, 0, 0, 0);
  }

  @Override
  public SelectionVectorMode[] getSupportedEncodings() {
    return SelectionVectorMode.DEFAULT;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitPrel(this, value);
  }

  @Override
  public Iterator<Prel> iterator() {
    return PrelUtil.iter(getLeft(), getRight());
  }

  @Override
  public boolean needsFinalColumnReordering() {
    return false;
  }

  public void setEstimatedRowCount(double rowCount) {
    estimatedRowCount = rowCount;
  }
}
