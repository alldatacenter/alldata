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

import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.common.logical.data.JoinCondition;

import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.HashJoinPOP;
import org.apache.drill.exec.physical.impl.join.JoinUtils;
import org.apache.drill.exec.physical.impl.join.JoinUtils.JoinCategory;
import org.apache.drill.exec.planner.common.JoinControl;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.work.filter.RuntimeFilterDef;

public class HashJoinPrel  extends JoinPrel {

  private boolean swapped = false;
  private RuntimeFilterDef runtimeFilterDef;
  protected boolean isRowKeyJoin = false;
  private int joinControl;

  public HashJoinPrel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
                      JoinRelType joinType, boolean semiJoin) throws InvalidRelException {
    this(cluster, traits, left, right, condition, joinType, false, null, false, JoinControl.DEFAULT, semiJoin);
  }

  public HashJoinPrel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
                      JoinRelType joinType, boolean swapped, RuntimeFilterDef runtimeFilterDef,
                      boolean isRowKeyJoin, int joinControl) throws InvalidRelException {
    this(cluster, traits, left, right, condition, joinType, swapped, runtimeFilterDef, isRowKeyJoin, joinControl, false);
  }

  public HashJoinPrel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
      JoinRelType joinType, boolean swapped, RuntimeFilterDef runtimeFilterDef,
      boolean isRowKeyJoin, int joinControl, boolean semiJoin) throws InvalidRelException {
    super(cluster, traits, left, right, condition, joinType, semiJoin);
    Preconditions.checkArgument(isSemiJoin && !swapped || swapped && !isSemiJoin || (!swapped && !isSemiJoin));
    if (isSemiJoin) {
      Preconditions.checkArgument(!swapped, "swapping of inputs is not allowed for semi-joins");
      Preconditions.checkArgument(validateTraits(traitSet, left, right));
    }
    this.swapped = swapped;
    this.isRowKeyJoin = isRowKeyJoin;
    joincategory = JoinUtils.getJoinCategory(left, right, condition, leftKeys, rightKeys, filterNulls);
    this.runtimeFilterDef = runtimeFilterDef;
    this.joinControl = joinControl;
  }

  private static boolean validateTraits(RelTraitSet traitSet, RelNode left, RelNode right) {
    ImmutableBitSet bitSet = ImmutableBitSet.range(left.getRowType().getFieldCount(),
            left.getRowType().getFieldCount() + right.getRowType().getFieldCount());
    for (RelTrait trait: traitSet) {
      if (trait.getTraitDef().getTraitClass().equals(RelCollation.class)) {
        RelCollation collationTrait = (RelCollation)trait;
        for (RelFieldCollation field : collationTrait.getFieldCollations()) {
          if (bitSet.indexOf(field.getFieldIndex()) > 0) {
            return false;
          }
        }
      } else if (trait.getTraitDef().getTraitClass().equals(DrillDistributionTrait.class)) {
        DrillDistributionTrait distributionTrait = (DrillDistributionTrait) trait;
        for (DrillDistributionTrait.DistributionField field : distributionTrait.getFields()) {
          if (bitSet.indexOf(field.getFieldId()) > 0) {
            return false;
          }
        }
      }
    }
    return true;
  }

  @Override
  public Join copy(RelTraitSet traitSet, RexNode conditionExpr, RelNode left, RelNode right, JoinRelType joinType, boolean semiJoinDone) {
    try {
      return new HashJoinPrel(this.getCluster(), traitSet, left, right, conditionExpr, joinType, this.swapped, this.runtimeFilterDef,
          this.isRowKeyJoin, this.joinControl, this.isSemiJoin);
    }catch (InvalidRelException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
    if (PrelUtil.getSettings(getCluster()).useDefaultCosting()) {
      return super.computeSelfCost(planner, mq).multiplyBy(.1);
    }
    if (joincategory == JoinCategory.CARTESIAN || joincategory == JoinCategory.INEQUALITY) {
      return planner.getCostFactory().makeInfiniteCost();
    }
    return computeHashJoinCost(planner, mq);
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    // Depending on whether the left/right is swapped for hash inner join, pass in different
    // combinations of parameters.
    if (! swapped) {
      return getHashJoinPop(creator, left, right, leftKeys, rightKeys, isRowKeyJoin, joinControl);
    } else {
      return getHashJoinPop(creator, right, left, rightKeys, leftKeys, isRowKeyJoin, joinControl);
    }
  }

  @Override
  public SelectionVectorMode[] getSupportedEncodings() {
    return SelectionVectorMode.DEFAULT;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }

  private PhysicalOperator getHashJoinPop(PhysicalPlanCreator creator, RelNode left, RelNode right,
                                          List<Integer> leftKeys, List<Integer> rightKeys,
                                          boolean isRowKeyJoin, int htControl) throws IOException{
    final List<String> fields = getRowType().getFieldNames();
    assert isUnique(fields);

    final List<String> leftFields = left.getRowType().getFieldNames();
    final List<String> rightFields = right.getRowType().getFieldNames();

    PhysicalOperator leftPop = ((Prel)left).getPhysicalOperator(creator);
    PhysicalOperator rightPop = ((Prel)right).getPhysicalOperator(creator);

    JoinRelType jtype = this.getJoinType();

    List<JoinCondition> conditions = Lists.newArrayList();

    buildJoinConditions(conditions, leftFields, rightFields, leftKeys, rightKeys);

    RuntimeFilterDef runtimeFilterDef = this.getRuntimeFilterDef();
    HashJoinPOP hjoin = new HashJoinPOP(leftPop, rightPop, conditions, jtype, isSemiJoin, runtimeFilterDef, isRowKeyJoin, htControl);
    return creator.addMetadata(this, hjoin);
  }

  public void setSwapped(boolean swapped) {
    this.swapped = swapped;
  }

  public boolean isSwapped() {
    return this.swapped;
  }

  public RuntimeFilterDef getRuntimeFilterDef() {
    return runtimeFilterDef;
  }

  public void setRuntimeFilterDef(RuntimeFilterDef runtimeFilterDef) {
    this.runtimeFilterDef = runtimeFilterDef;
  }

  public boolean isRowKeyJoin() {
    return this.isRowKeyJoin;
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    return super.explainTerms(pw).item("semi-join: ", isSemiJoin);
  }
}
