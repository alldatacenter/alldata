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

import org.apache.calcite.plan.RelOptUtil;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.NestedLoopJoinPOP;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.planner.cost.DrillCostBase.DrillCostFactory;
import org.apache.drill.exec.planner.logical.DrillOptiq;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;

public class NestedLoopJoinPrel  extends JoinPrel {

  public NestedLoopJoinPrel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
                      JoinRelType joinType) throws InvalidRelException {
    super(cluster, traits, left, right, condition, joinType);
  }

  public NestedLoopJoinPrel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
                            JoinRelType joinType, boolean semijoin) throws InvalidRelException {
    super(cluster, traits, left, right, condition, joinType, semijoin);
  }

  @Override
  public Join copy(RelTraitSet traitSet, RexNode conditionExpr, RelNode left, RelNode right, JoinRelType joinType, boolean semiJoinDone) {
    try {
      return new NestedLoopJoinPrel(this.getCluster(), traitSet, left, right, conditionExpr, joinType);
    }catch (InvalidRelException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public double estimateRowCount(RelMetadataQuery mq) {
    return this.getLeft().estimateRowCount(mq) * this.getRight().estimateRowCount(mq);
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    if (PrelUtil.getSettings(getCluster()).useDefaultCosting()) {
      return super.computeSelfCost(planner, mq).multiplyBy(.1);
    }
    double leftRowCount = mq.getRowCount(this.getLeft());
    double rightRowCount = mq.getRowCount(this.getRight());
    double nljFactor = PrelUtil.getSettings(getCluster()).getNestedLoopJoinFactor();

    // cpu cost of evaluating each expression in join condition
    int exprNum = RelOptUtil.conjunctions(getCondition()).size() + RelOptUtil.disjunctions(getCondition()).size();
    double joinConditionCost = DrillCostBase.COMPARE_CPU_COST * exprNum;

    double cpuCost = joinConditionCost * (leftRowCount * rightRowCount) * nljFactor;

    DrillCostFactory costFactory = (DrillCostFactory) planner.getCostFactory();
    return costFactory.makeCost(leftRowCount * rightRowCount, cpuCost, 0, 0, 0);
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    PhysicalOperator leftPop = ((Prel)left).getPhysicalOperator(creator);
    PhysicalOperator rightPop = ((Prel)right).getPhysicalOperator(creator);

    /*
       Raw expression will be transformed into its logical representation. For example:
       Query:
         select t1.c1, t2.c1, t2.c2 from t1 inner join t2 on t1.c1 between t2.c1 and t2.c2
       Raw expression:
         AND(>=($0, $1), <=($0, $2))
       Logical expression:
         FunctionCall [func=booleanAnd,
         args=[FunctionCall [func=greater_than_or_equal_to, args=[`i1`, `i10`]],
               FunctionCall [func=less_than_or_equal_to, args=[`i1`, `i2`]]]

       Both tables have the same column name thus duplicated column name in second table are renamed: i1 -> i10.
    */
    LogicalExpression condition = DrillOptiq.toDrill(
        new DrillParseContext(PrelUtil.getSettings(getCluster())),
        getInputs(),
        getCondition());

    NestedLoopJoinPOP nlj = new NestedLoopJoinPOP(leftPop, rightPop, getJoinType(), condition);
    return creator.addMetadata(this, nlj);
  }

  @Override
  public SelectionVectorMode[] getSupportedEncodings() {
    return SelectionVectorMode.DEFAULT;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }

}
