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

import java.util.List;

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.physical.impl.join.JoinUtils;
import org.apache.drill.exec.physical.impl.join.JoinUtils.JoinCategory;
import org.apache.drill.exec.planner.common.DrillJoinRelBase;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.common.JoinControl;
import org.apache.drill.exec.planner.logical.DrillJoin;
import org.apache.drill.exec.planner.logical.DrillPushRowKeyJoinToScanRule;
import org.apache.drill.exec.planner.logical.RowKeyJoinRel;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionField;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rex.RexNode;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

// abstract base class for the join physical rules
public abstract class JoinPruleBase extends Prule {

  protected enum PhysicalJoinType {HASH_JOIN, MERGE_JOIN, NESTEDLOOP_JOIN}

  protected JoinPruleBase(RelOptRuleOperand operand, String description) {
    super(operand, description);
  }

  protected boolean checkPreconditions(DrillJoin join, RelNode left, RelNode right,
                                       PlannerSettings settings) {
    List<Integer> leftKeys = Lists.newArrayList();
    List<Integer> rightKeys = Lists.newArrayList();
    List<Boolean> filterNulls = Lists.newArrayList();
    JoinCategory category = JoinUtils.getJoinCategory(left, right, join.getCondition(), leftKeys, rightKeys, filterNulls);
    return !(category == JoinCategory.CARTESIAN || category == JoinCategory.INEQUALITY);
  }

  protected List<DistributionField> getDistributionField(List<Integer> keys) {
    List<DistributionField> distFields = Lists.newArrayList();

    for (int key : keys) {
      distFields.add(new DistributionField(key));
    }

    return distFields;
  }

  protected boolean checkBroadcastConditions(RelOptPlanner planner, DrillJoin join, RelNode left, RelNode right) {

    double estimatedRightRowCount = RelMetadataQuery.instance().getRowCount(right);
    if (estimatedRightRowCount < PrelUtil.getSettings(join.getCluster()).getBroadcastThreshold()
        && ! left.getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE).equals(DrillDistributionTrait.SINGLETON)
        && (join.getJoinType() == JoinRelType.INNER || join.getJoinType() == JoinRelType.LEFT)
        ) {
      return true;
    }
    return false;
  }

  protected void createRangePartitionRightPlan(RelOptRuleCall call, RowKeyJoinRel join,
    PhysicalJoinType physicalJoinType, boolean implementAsRowKeyJoin, RelNode left, RelNode right,
    RelCollation collationLeft, RelCollation collationRight) throws InvalidRelException {
    assert join.getRightKeys().size() == 1 : "Cannot create range partition plan with multi-column join condition";
    int joinKeyRight = join.getRightKeys().get(0);
    List<DrillDistributionTrait.DistributionField> rangeDistFields =
        Lists.newArrayList(new DrillDistributionTrait.DistributionField(joinKeyRight /* `rowkey equivalent` ordinal on the right side */));
    List<FieldReference> rangeDistRefList = Lists.newArrayList();
    FieldReference rangeDistRef =
        FieldReference.getWithQuotedRef(right.getRowType().getFieldList().get(joinKeyRight).getName());
    rangeDistRefList.add(rangeDistRef);
    RelNode leftScan = DrillPushRowKeyJoinToScanRule.getValidJoinInput(left);
    DrillDistributionTrait rangePartRight = new DrillDistributionTrait(
        DrillDistributionTrait.DistributionType.RANGE_DISTRIBUTED,
        ImmutableList.copyOf(rangeDistFields),
        ((DbGroupScan)((DrillScanRelBase) leftScan).getGroupScan()).getRangePartitionFunction(rangeDistRefList));

    RelTraitSet traitsLeft = null;
    RelTraitSet traitsRight = null;

    if (physicalJoinType == PhysicalJoinType.HASH_JOIN) {
      traitsLeft = left.getTraitSet().plus(Prel.DRILL_PHYSICAL);
      traitsRight = right.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(rangePartRight);
    }

    final RelNode convertedLeft = convert(left, traitsLeft);
    final RelNode convertedRight = convert(right, traitsRight);

    DrillJoinRelBase newJoin = null;

    if (physicalJoinType == PhysicalJoinType.HASH_JOIN) {
      if (implementAsRowKeyJoin) {
        newJoin = new RowKeyJoinPrel(join.getCluster(), traitsLeft,
            convertedLeft, convertedRight, join.getCondition(),
            join.getJoinType(), join.isSemiJoin());
      } else {
        newJoin = new HashJoinPrel(join.getCluster(), traitsLeft,
            convertedLeft, convertedRight, join.getCondition(),
            join.getJoinType(), false /* no swap */,
            null /* no runtime filter */,
            true /* useful for join-restricted scans */,
            JoinControl.DEFAULT, join.isSemiJoin());
      }
    }
    if (newJoin != null) {
      call.transformTo(newJoin);
    }
  }

  protected void createDistBothPlan(RelOptRuleCall call, DrillJoin join,
      PhysicalJoinType physicalJoinType,
      RelNode left, RelNode right,
      RelCollation collationLeft, RelCollation collationRight,
      boolean hashSingleKey)throws InvalidRelException {

    /* If join keys are  l1 = r1 and l2 = r2 and ... l_k = r_k, then consider the following options of plan:
     *   1) Plan1: distributed by (l1, l2, ..., l_k) for left side and by (r1, r2, ..., r_k) for right side.
     *   2) Plan2: distributed by l1 for left side, by r1 for right side.
     *   3) Plan3: distributed by l2 for left side, by r2 for right side.
     *   ...
     *      Plan_(k+1): distributed by l_k for left side, by r_k by right side.
     *
     *   Whether enumerate plan 2, .., Plan_(k+1) depends on option : hashSingleKey.
     */

    DrillDistributionTrait hashLeftPartition = new DrillDistributionTrait(DrillDistributionTrait.DistributionType.HASH_DISTRIBUTED,
            ImmutableList.copyOf(getDistributionField(join.getLeftKeys())));
    DrillDistributionTrait hashRightPartition = new DrillDistributionTrait(DrillDistributionTrait.DistributionType.HASH_DISTRIBUTED,
            ImmutableList.copyOf(getDistributionField(join.getRightKeys())));

    createDistBothPlan(call, join, physicalJoinType, left, right, collationLeft, collationRight, hashLeftPartition, hashRightPartition);

    assert (join.getLeftKeys().size() == join.getRightKeys().size());

    if (!hashSingleKey) {
      return;
    }

    int numJoinKeys = join.getLeftKeys().size();
    if (numJoinKeys > 1) {
      for (int i = 0; i< numJoinKeys; i++) {
        hashLeftPartition = new DrillDistributionTrait(DrillDistributionTrait.DistributionType.HASH_DISTRIBUTED, ImmutableList.copyOf(getDistributionField(join.getLeftKeys().subList(i, i+1))));
        hashRightPartition = new DrillDistributionTrait(DrillDistributionTrait.DistributionType.HASH_DISTRIBUTED, ImmutableList.copyOf(getDistributionField(join.getRightKeys().subList(i, i+1))));

        createDistBothPlan(call, join, physicalJoinType, left, right, collationLeft, collationRight, hashLeftPartition, hashRightPartition);
      }
    }
  }

  // Create join plan with both left and right children hash distributed. If the physical join type
  // is MergeJoin, a collation must be provided for both left and right child and the plan will contain
  // sort converter if necessary to provide the collation.
  private void createDistBothPlan(RelOptRuleCall call, DrillJoin join,
      PhysicalJoinType physicalJoinType,
      RelNode left, RelNode right,
      RelCollation collationLeft, RelCollation collationRight,
      DrillDistributionTrait hashLeftPartition, DrillDistributionTrait hashRightPartition) throws InvalidRelException {

    RelTraitSet traitsLeft = null;
    RelTraitSet traitsRight = null;

    if (physicalJoinType == PhysicalJoinType.MERGE_JOIN) {
      assert collationLeft != null && collationRight != null;
      traitsLeft = left.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(collationLeft).plus(hashLeftPartition);
      traitsRight = right.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(collationRight).plus(hashRightPartition);
    } else if (physicalJoinType == PhysicalJoinType.HASH_JOIN) {
      traitsLeft = left.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(hashLeftPartition);
      traitsRight = right.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(hashRightPartition);
    }

    final RelNode convertedLeft = convert(left, traitsLeft);
    final RelNode convertedRight = convert(right, traitsRight);

    DrillJoinRelBase newJoin = null;

    if (physicalJoinType == PhysicalJoinType.HASH_JOIN) {
      final RelTraitSet traitSet = PrelUtil.removeCollation(traitsLeft, call);
      newJoin = new HashJoinPrel(join.getCluster(), traitSet,
                                 convertedLeft, convertedRight, join.getCondition(),
                                 join.getJoinType(), join.isSemiJoin());

    } else if (physicalJoinType == PhysicalJoinType.MERGE_JOIN) {
      newJoin = new MergeJoinPrel(join.getCluster(), traitsLeft,
                                  convertedLeft, convertedRight, join.getCondition(),
                                  join.getJoinType(), join.isSemiJoin());
    }
    call.transformTo(newJoin);
  }

  // Create join plan with left child ANY distributed and right child BROADCAST distributed. If the physical join type
  // is MergeJoin, a collation must be provided for both left and right child and the plan will contain sort converter
  // if necessary to provide the collation.
  protected void createBroadcastPlan(final RelOptRuleCall call, final DrillJoin join,
      final RexNode joinCondition,
      final PhysicalJoinType physicalJoinType,
      final RelNode left, final RelNode right,
      final RelCollation collationLeft, final RelCollation collationRight) throws InvalidRelException {

    DrillDistributionTrait distBroadcastRight = new DrillDistributionTrait(DrillDistributionTrait.DistributionType.BROADCAST_DISTRIBUTED);
    RelTraitSet traitsRight = null;
    RelTraitSet traitsLeft = left.getTraitSet().plus(Prel.DRILL_PHYSICAL);

    if (physicalJoinType == PhysicalJoinType.MERGE_JOIN) {
      assert collationLeft != null && collationRight != null;
      traitsLeft = traitsLeft.plus(collationLeft);
      traitsRight = right.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(collationRight).plus(distBroadcastRight);
    } else if (physicalJoinType == PhysicalJoinType.HASH_JOIN ||
        physicalJoinType == PhysicalJoinType.NESTEDLOOP_JOIN) {
      traitsRight = right.getTraitSet().plus(Prel.DRILL_PHYSICAL).plus(distBroadcastRight);
    }

    final RelNode convertedLeft = convert(left, traitsLeft);
    final RelNode convertedRight = convert(right, traitsRight);

    boolean traitProp = false;

    if(traitProp){
      if (physicalJoinType == PhysicalJoinType.MERGE_JOIN) {
        new SubsetTransformer<DrillJoin, InvalidRelException>(call) {

          @Override
          public RelNode convertChild(final DrillJoin join, final RelNode rel) throws InvalidRelException {
            DrillDistributionTrait toDist = rel.getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE);
            RelTraitSet newTraitsLeft = newTraitSet(Prel.DRILL_PHYSICAL, collationLeft, toDist);

            RelNode newLeft = convert(left, newTraitsLeft);
              return new MergeJoinPrel(join.getCluster(), newTraitsLeft, newLeft, convertedRight, joinCondition,
                                          join.getJoinType());
          }

        }.go(join, convertedLeft);


      } else if (physicalJoinType == PhysicalJoinType.HASH_JOIN) {
        new SubsetTransformer<DrillJoin, InvalidRelException>(call) {

          @Override
          public RelNode convertChild(final DrillJoin join,  final RelNode rel) throws InvalidRelException {
            DrillDistributionTrait toDist = rel.getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE);
            RelTraitSet newTraitsLeft = newTraitSet(Prel.DRILL_PHYSICAL, toDist);
            RelNode newLeft = convert(left, newTraitsLeft);
            return new HashJoinPrel(join.getCluster(), newTraitsLeft, newLeft, convertedRight, joinCondition,
                                         join.getJoinType(), join.isSemiJoin());

          }

        }.go(join, convertedLeft);
      } else if (physicalJoinType == PhysicalJoinType.NESTEDLOOP_JOIN) {
        new SubsetTransformer<DrillJoin, InvalidRelException>(call) {

          @Override
          public RelNode convertChild(final DrillJoin join,  final RelNode rel) throws InvalidRelException {
            DrillDistributionTrait toDist = rel.getTraitSet().getTrait(DrillDistributionTraitDef.INSTANCE);
            RelTraitSet newTraitsLeft = newTraitSet(Prel.DRILL_PHYSICAL, toDist);
            RelNode newLeft = convert(left, newTraitsLeft);
            return new NestedLoopJoinPrel(join.getCluster(), newTraitsLeft, newLeft, convertedRight, joinCondition,
                                         join.getJoinType());
          }

        }.go(join, convertedLeft);
      }

    } else {
      if (physicalJoinType == PhysicalJoinType.MERGE_JOIN) {
        call.transformTo(new MergeJoinPrel(join.getCluster(), convertedLeft.getTraitSet(), convertedLeft,
            convertedRight, joinCondition, join.getJoinType(), join.isSemiJoin()));
      } else if (physicalJoinType == PhysicalJoinType.HASH_JOIN) {
        final RelTraitSet traitSet = PrelUtil.removeCollation(convertedLeft.getTraitSet(), call);
        call.transformTo(new HashJoinPrel(join.getCluster(), traitSet, convertedLeft,
            convertedRight, joinCondition, join.getJoinType(), join.isSemiJoin()));
      } else if (physicalJoinType == PhysicalJoinType.NESTEDLOOP_JOIN) {
        call.transformTo(new NestedLoopJoinPrel(join.getCluster(), convertedLeft.getTraitSet(), convertedLeft,
            convertedRight, joinCondition, join.getJoinType()));
      }
    }

  }
}
