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
package org.apache.drill.exec.expr;

import java.util.Iterator;
import java.util.List;

import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;

/**
 * Represents the call of a function within a query and includes
 * the actual arguments and a reference to the function declaration (as a
 * "function holder.")
 */
public class DrillFuncHolderExpr extends FunctionHolderExpression
        implements Iterable<LogicalExpression> {

  private final DrillFuncHolder holder;
  private final MajorType majorType;
  private DrillSimpleFunc interpreter;

  public DrillFuncHolderExpr(String nameUsed, DrillFuncHolder holder,
      List<LogicalExpression> args, ExpressionPosition pos) {
    super(nameUsed, pos, args);
    this.holder = holder;
    // since function return type can not be changed, cache it for better performance
    this.majorType = holder.getReturnType(args);
  }

  @Override
  public MajorType getMajorType() {
    return majorType;
  }

  @Override
  public Iterator<LogicalExpression> iterator() {
    return args.iterator();
  }

  @Override
  public DrillFuncHolder getHolder() {
    return holder;
  }

  @Override
  public boolean isAggregating() {
    return holder.isAggregating();
  }

  @Override
  public boolean isRandom() {
    return !holder.isDeterministic();
  }

  @Override
  public boolean argConstantOnly(int i) {
    return holder.isConstant(i);
  }

  @Override
  public int getSelfCost() {
    return holder.getCostCategory();
  }

  @Override
  public int getCumulativeCost() {
    int cost = getSelfCost();

    for (LogicalExpression arg : args) {
      cost += arg.getCumulativeCost();
    }

    return cost;
  }

  @Override
  public DrillFuncHolderExpr copy(List<LogicalExpression> args) {
    return new DrillFuncHolderExpr(nameUsed, holder, args, getPosition());
  }

  public void setInterpreter(DrillSimpleFunc interpreter) {
    this.interpreter = interpreter;
  }

  public DrillSimpleFunc getInterpreter() {
    return interpreter;
  }
}

