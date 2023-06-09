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
package org.apache.drill.exec.planner.logical;


import java.util.List;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexChecker;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.util.Litmus;
import org.apache.calcite.util.Pair;
import org.apache.drill.common.logical.data.Join;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.exec.planner.torel.ConversionContext;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

public class RowKeyJoinRel extends DrillJoinRel implements DrillRel {

  /* Whether this join represents a semi-join. This is done to skip creating another logical join
   * RowKeySemiJoinRel
   */
  boolean isSemiJoin;

  public RowKeyJoinRel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
                      JoinRelType joinType)  {
    super(cluster, traits, left, right, condition, joinType);
  }

  public RowKeyJoinRel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
                       JoinRelType joinType, boolean isSemiJoin)  {
    super(cluster, traits, left, right, condition, joinType);
    this.isSemiJoin = isSemiJoin;
  }

  public RowKeyJoinRel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
                      JoinRelType joinType, int joinControl)  {
    super(cluster, traits, left, right, condition, joinType, joinControl);
  }

  public RowKeyJoinRel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
                      JoinRelType joinType, List<Integer> leftKeys, List<Integer> rightKeys) throws InvalidRelException {
    super(cluster, traits, left, right, condition, joinType, leftKeys, rightKeys);
  }

  @Override
  public RowKeyJoinRel copy(RelTraitSet traitSet, RexNode condition, RelNode left, RelNode right, JoinRelType joinType,
      boolean semiJoinDone) {
    return new RowKeyJoinRel(getCluster(), traitSet, left, right, condition, joinType, isSemiJoin());
  }

  @Override
  public LogicalOperator implement(DrillImplementor implementor) {
    return super.implement(implementor);
  }

  /**
   * Returns whether this RowKeyJoin represents a {@link org.apache.calcite.rel.core.SemiJoin}
   * @return true if join represents a {@link org.apache.calcite.rel.core.SemiJoin}, false otherwise.
   */
  public boolean isSemiJoin() {
    return isSemiJoin;
  }

  @Override
  public RelDataType deriveRowType() {
    return SqlValidatorUtil.deriveJoinRowType(
            left.getRowType(),
            isSemiJoin() ? null : right.getRowType(),
            JoinRelType.INNER,
            getCluster().getTypeFactory(),
            null,
            ImmutableList.of());
  }

  public static RowKeyJoinRel convert(Join join, ConversionContext context) throws InvalidRelException {
    Pair<RelNode, RelNode> inputs = getJoinInputs(join, context);
    RexNode rexCondition = getJoinCondition(join, context);
    RowKeyJoinRel joinRel = new RowKeyJoinRel(context.getCluster(), context.getLogicalTraits(),
        inputs.left, inputs.right, rexCondition, join.getJoinType());
    return joinRel;
  }

  /** The parent method relies the class being an instance of {@link org.apache.calcite.rel.core.SemiJoin}
   * in deciding row-type validity. We override this method to account for the RowKeyJoinRel logical rel
   * representing both regular and semi-joins */
  @Override public boolean isValid(Litmus litmus, Context context) {
    if (getRowType().getFieldCount()
            != getSystemFieldList().size()
            + left.getRowType().getFieldCount()
            + ((this.isSemiJoin()) ? 0 : right.getRowType().getFieldCount())) {
      return litmus.fail("field count mismatch");
    }
    if (condition != null) {
      if (condition.getType().getSqlTypeName() != SqlTypeName.BOOLEAN) {
        return litmus.fail("condition must be boolean: {}",
                condition.getType());
      }
      // The input to the condition is a row type consisting of system
      // fields, left fields, and right fields. Very similar to the
      // output row type, except that fields have not yet been made due
      // due to outer joins.
      RexChecker checker =
              new RexChecker(
                      getCluster().getTypeFactory().builder()
                              .addAll(getSystemFieldList())
                              .addAll(getLeft().getRowType().getFieldList())
                              .addAll(getRight().getRowType().getFieldList())
                              .build(),
                      context, litmus);
      condition.accept(checker);
      if (checker.getFailureCount() > 0) {
        return litmus.fail(checker.getFailureCount()
                + " failures in condition " + condition);
      }
    }
    return litmus.succeed();
  }
}
