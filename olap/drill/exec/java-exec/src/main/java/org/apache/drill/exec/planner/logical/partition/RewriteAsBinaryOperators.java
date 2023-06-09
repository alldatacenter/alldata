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
package org.apache.drill.exec.planner.logical.partition;

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexCorrelVariable;
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexFieldAccess;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexLocalRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexOver;
import org.apache.calcite.rex.RexRangeRef;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlOperator;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Rewrites an expression tree, replacing OR and AND operators with more than 2 operands with a chained operators
 * each with only 2 operands.
 *
 * e.g.
 *
 * OR(A, B, C) ---> OR(A, OR(B, C))
 */
 public class RewriteAsBinaryOperators extends RexVisitorImpl<RexNode> {

  RexBuilder builder;
  public RewriteAsBinaryOperators(boolean deep, RexBuilder builder) {
    super(deep);
    this.builder = builder;
  }

  @Override
  public RexNode visitInputRef(RexInputRef inputRef) {
    return inputRef;
  }

  @Override
  public RexNode visitLiteral(RexLiteral literal) {
    return literal;
  }

  @Override
  public RexNode visitOver(RexOver over) {
    return over;
  }

  @Override
  public RexNode visitCorrelVariable(RexCorrelVariable correlVariable) {
    return correlVariable;
  }

  @Override
  public RexNode visitCall(RexCall call) {
    SqlOperator op = call.getOperator();
    SqlKind kind = op.getKind();
    RelDataType type = call.getType();
    if (kind == SqlKind.OR || kind == SqlKind.AND) {
      if (call.getOperands().size() > 2) {
        List<RexNode> children = new ArrayList<>(call.getOperands());
        RexNode left = children.remove(0).accept(this);
        RexNode right = builder.makeCall(type, op, children).accept(this);
        return builder.makeCall(type, op, ImmutableList.of(left, right));
      }
    }
    return builder.makeCall(type, op, visitChildren(call));
  }

  private List<RexNode> visitChildren(RexCall call) {
    List<RexNode> children = Lists.newArrayList();
    for (RexNode child : call.getOperands()) {
      children.add(child.accept(this));
    }
    return ImmutableList.copyOf(children);
  }

  @Override
  public RexNode visitDynamicParam(RexDynamicParam dynamicParam) {
    return dynamicParam;
  }

  @Override
  public RexNode visitRangeRef(RexRangeRef rangeRef) {
    return rangeRef;
  }

  @Override
  public RexNode visitFieldAccess(RexFieldAccess fieldAccess) {
    return fieldAccess;
  }

  @Override
  public RexNode visitLocalRef(RexLocalRef localRef) {
    return localRef;
  }
}
