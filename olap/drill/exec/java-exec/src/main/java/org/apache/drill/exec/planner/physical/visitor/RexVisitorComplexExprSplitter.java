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
package org.apache.drill.exec.planner.physical.visitor;

import java.util.ArrayList;
import java.util.List;

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
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;

public class RexVisitorComplexExprSplitter extends RexVisitorImpl<RexNode> {

  private final FunctionImplementationRegistry funcReg;
  private final RexBuilder rexBuilder;
  private final List<RexNode> complexExprs;

  private int lastUsedIndex;

  public RexVisitorComplexExprSplitter(FunctionImplementationRegistry funcReg,
                                       RexBuilder rexBuilder,
                                       int firstUnused) {
    super(true);
    this.funcReg = funcReg;
    this.rexBuilder = rexBuilder;
    this.complexExprs = new ArrayList<>();
    this.lastUsedIndex = firstUnused;
  }

  public  List<RexNode> getComplexExprs() {
    return complexExprs;
  }

  @Override
  public RexNode visitInputRef(RexInputRef inputRef) {
    return inputRef;
  }

  @Override
  public RexNode visitLocalRef(RexLocalRef localRef) {
    return localRef;
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
    final List<RexNode> newOps = new ArrayList<>();
    for (RexNode operand : call.operands) {
      RexNode newOp = operand.accept(this);
      newOps.add(newOp);
    }
    final RexCall newCall = call.clone(call.getType(), newOps);

    String functionName = call.getOperator().getName();
    if (funcReg.isFunctionComplexOutput(functionName)) {
      RexNode ret = rexBuilder.makeInputRef(newCall.getType(), lastUsedIndex++);
      complexExprs.add(newCall);
      return ret;
    } else {
      return newCall;
    }
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

}
