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

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.util.Pair;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.logical.data.Join;
import org.apache.drill.common.logical.data.JoinCondition;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.Project;
import org.apache.drill.exec.planner.common.DrillJoinRelBase;
import org.apache.drill.exec.planner.common.JoinControl;
import org.apache.drill.exec.planner.torel.ConversionContext;

/**
 * Logical Join implemented in Drill.
 */
public class DrillJoinRel extends DrillJoinRelBase implements DrillRel {
  public static final String EQUALITY_CONDITION = "==";

  /** Creates a DrillJoinRel.
   * We do not throw InvalidRelException in Logical planning phase. It's up to the post-logical planning check or physical planning
   * to detect the unsupported join type, and throw exception.
   * */
  private int joinControl = JoinControl.DEFAULT;

  public DrillJoinRel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
      JoinRelType joinType) {
    super(cluster, traits, left, right, condition, joinType);
    assert traits.contains(DrillRel.DRILL_LOGICAL);
    RelOptUtil.splitJoinCondition(left, right, condition, leftKeys, rightKeys, filterNulls);
  }

  public DrillJoinRel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
      JoinRelType joinType, int joinControl)  {
    super(cluster, traits, left, right, condition, joinType);
    assert traits.contains(DrillRel.DRILL_LOGICAL);
    RelOptUtil.splitJoinCondition(left, right, condition, leftKeys, rightKeys, filterNulls);
    this.joinControl = joinControl;
  }

  public DrillJoinRel(RelOptCluster cluster, RelTraitSet traits, RelNode left, RelNode right, RexNode condition,
      JoinRelType joinType, List<Integer> leftKeys, List<Integer> rightKeys) throws InvalidRelException {
    super(cluster, traits, left, right, condition, joinType);
    assert traits.contains(DrillRel.DRILL_LOGICAL);

    assert (leftKeys != null && rightKeys != null);
    this.leftKeys = leftKeys;
    this.rightKeys = rightKeys;
  }

  @Override
  public DrillJoinRel copy(RelTraitSet traitSet, RexNode condition, RelNode left, RelNode right, JoinRelType joinType, boolean semiJoinDone) {
    return new DrillJoinRel(getCluster(), traitSet, left, right, condition, joinType);
  }

  @Override
  public LogicalOperator implement(DrillImplementor implementor) {
    final List<String> fields = getRowType().getFieldNames();
    assert isUnique(fields);
    final int leftCount = left.getRowType().getFieldCount();
    final List<String> leftFields = fields.subList(0, leftCount);
    final List<String> rightFields = fields.subList(leftCount, fields.size());

    final LogicalOperator leftOp = implementInput(implementor, 0, 0, left);
    final LogicalOperator rightOp = implementInput(implementor, 1, leftCount, right);

    Join.Builder builder = Join.builder();
    builder.type(joinType);
    builder.left(leftOp);
    builder.right(rightOp);

    for (Pair<Integer, Integer> pair : Pair.zip(leftKeys, rightKeys)) {
      builder.addCondition(EQUALITY_CONDITION, new FieldReference(leftFields.get(pair.left)), new FieldReference(rightFields.get(pair.right)));
    }

    return builder.build();
  }

  /**
   * Check to make sure that the fields of the inputs are the same as the output field names.  If not, insert a project renaming them.
   * @param implementor
   * @param i
   * @param offset
   * @param input
   * @return
   */
  private LogicalOperator implementInput(DrillImplementor implementor, int i, int offset, RelNode input) {
    return implementInput(implementor, i, offset, input, this, this.getRowType().getFieldNames());
  }

  /**
   * Check to make sure that the fields of the inputs are the same as the output field names.
   * If not, insert a project renaming them.
   * @param implementor
   * @param i
   * @param offset
   * @param input
   * @param currentNode the node to be implemented
   * @return
   */
  public static LogicalOperator implementInput(DrillImplementor implementor, int i, int offset,
                                               RelNode input, DrillRel currentNode,
                                               List<String> parentFields) {
    final LogicalOperator inputOp = implementor.visitChild(currentNode, i, input);
    assert uniqueFieldNames(input.getRowType());
    final List<String> inputFields = input.getRowType().getFieldNames();
    final List<String> outputFields = parentFields.subList(offset, offset + inputFields.size());
    if (!outputFields.equals(inputFields)) {
      // Ensure that input field names are the same as output field names.
      // If there are duplicate field names on left and right, fields will get
      // lost.
      return rename(implementor, inputOp, inputFields, outputFields);
    } else {
      return inputOp;
    }
  }

  private static LogicalOperator rename(DrillImplementor implementor, LogicalOperator inputOp,
                                        List<String> inputFields, List<String> outputFields) {
    Project.Builder builder = Project.builder();
    builder.setInput(inputOp);
    for (Pair<String, String> pair : Pair.zip(inputFields, outputFields)) {
      builder.addExpr(new FieldReference(pair.right), new FieldReference(pair.left));
    }
    return builder.build();
  }

  protected static Pair<RelNode, RelNode> getJoinInputs(Join join, ConversionContext context) throws InvalidRelException {
    RelNode left = context.toRel(join.getLeft());
    RelNode right = context.toRel(join.getRight());
    return Pair.of(left, right);
  }

  protected static RexNode getJoinCondition(Join join, ConversionContext context) throws InvalidRelException {
    Pair<RelNode, RelNode> inputs = getJoinInputs(join, context);
    List<RexNode> joinConditions = new ArrayList<RexNode>();
    // right fields appear after the LHS fields.
    final int rightInputOffset = inputs.left.getRowType().getFieldCount();
    for (JoinCondition condition : join.getConditions()) {
      RelDataTypeField leftField = inputs.left.getRowType().getField(ExprHelper.getFieldName(condition.getLeft()),
          true, false);
      RelDataTypeField rightField = inputs.right.getRowType().getField(ExprHelper.getFieldName(condition.getRight()),
          true, false);
      joinConditions.add(
          context.getRexBuilder().makeCall(
              SqlStdOperatorTable.EQUALS,
              context.getRexBuilder().makeInputRef(leftField.getType(), leftField.getIndex()),
              context.getRexBuilder().makeInputRef(rightField.getType(), rightInputOffset + rightField.getIndex())
          )
      );
    }
    RexNode rexCondition = RexUtil.composeConjunction(context.getRexBuilder(), joinConditions, false);
    return rexCondition;
  }

  public static DrillJoinRel convert(Join join, ConversionContext context) throws InvalidRelException{
    Pair<RelNode, RelNode> inputs = getJoinInputs(join, context);
    RexNode rexCondition = getJoinCondition(join, context);
    DrillJoinRel joinRel = new DrillJoinRel(context.getCluster(), context.getLogicalTraits(),
        inputs.left, inputs.right, rexCondition, join.getJoinType());
    return joinRel;
  }
}
