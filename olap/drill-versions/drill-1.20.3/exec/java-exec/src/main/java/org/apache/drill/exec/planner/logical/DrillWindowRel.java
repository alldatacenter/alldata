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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.linq4j.Ord;
import org.apache.calcite.util.BitSets;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.Order;
import org.apache.drill.exec.planner.common.DrillWindowRelBase;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexLiteral;

import java.util.List;

public class DrillWindowRel extends DrillWindowRelBase implements DrillRel {
  /**
   * Creates a window relational expression.
   *
   * @param cluster Cluster
   * @param traits
   * @param child   Input relational expression
   * @param rowType Output row type
   * @param groups Windows
   */
  public DrillWindowRel(
      RelOptCluster cluster,
      RelTraitSet traits,
      RelNode child,
      List<RexLiteral> constants,
      RelDataType rowType,
      List<Group> groups) {
    super(cluster, traits, child, constants, rowType, groups);
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new DrillWindowRel(getCluster(), traitSet, sole(inputs), constants, getRowType(), groups);
  }

  @Override
  public LogicalOperator implement(DrillImplementor implementor) {
    final LogicalOperator inputOp = implementor.visitChild(this, 0, getInput());
    org.apache.drill.common.logical.data.Window.Builder builder = new org.apache.drill.common.logical.data.Window.Builder();
    final List<String> fields = getRowType().getFieldNames();
    final List<String> childFields = getInput().getRowType().getFieldNames();
    for (Group window : groups) {

      for(RelFieldCollation orderKey : window.orderKeys.getFieldCollations()) {
        builder.addOrdering(new Order.Ordering(orderKey.getDirection(), new FieldReference(fields.get(orderKey.getFieldIndex()))));
      }

      for (int group : BitSets.toIter(window.keys)) {
        FieldReference fr = new FieldReference(childFields.get(group), ExpressionPosition.UNKNOWN);
        builder.addWithin(fr, fr);
      }

      int groupCardinality = window.keys.cardinality();
      for (Ord<AggregateCall> aggCall : Ord.zip(window.getAggregateCalls(this))) {
        FieldReference ref = new FieldReference(fields.get(groupCardinality + aggCall.i));
        LogicalExpression expr = toDrill(aggCall.e, childFields);
        builder.addAggregation(ref, expr);
      }
    }
    builder.setInput(inputOp);
    org.apache.drill.common.logical.data.Window frame = builder.build();
    return frame;
  }

  protected LogicalExpression toDrill(AggregateCall call, List<String> fn) {
    List<LogicalExpression> args = Lists.newArrayList();
    for (Integer i : call.getArgList()) {
      args.add(new FieldReference(fn.get(i)));
    }

    // for count(1).
    if (args.isEmpty()) {
      args.add(new ValueExpressions.LongExpression(1l));
    }
    LogicalExpression expr = new FunctionCall(call.getAggregation().getName().toLowerCase(), args, ExpressionPosition.UNKNOWN);
    return expr;
  }
}


