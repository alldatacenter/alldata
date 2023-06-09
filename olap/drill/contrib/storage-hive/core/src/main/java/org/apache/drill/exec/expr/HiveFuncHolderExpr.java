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
import org.apache.drill.common.expression.fn.FuncHolder;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.fn.HiveFuncHolder;

public class HiveFuncHolderExpr extends FunctionHolderExpression implements Iterable<LogicalExpression>{

  private final HiveFuncHolder holder;

  public HiveFuncHolderExpr(String nameUsed, HiveFuncHolder holder, List<LogicalExpression> args, ExpressionPosition pos) {
    super(nameUsed, pos, args);
    this.holder = holder;
  }

  @Override
  public MajorType getMajorType() {
    return holder.getReturnType();
  }

  @Override
  public Iterator<LogicalExpression> iterator() {
    return args.iterator();
  }

  @Override
  public FuncHolder getHolder() {
    return holder;
  }

  @Override
  public boolean isAggregating() {
    return holder.isAggregating();
  }

  @Override
  public boolean argConstantOnly(int i) {
    // looks like hive UDF has no notion of constant argument input
    return false;
  }

  @Override
  public boolean isRandom() {
    return holder.isRandom();
  }

  @Override
  public HiveFuncHolderExpr copy(List<LogicalExpression> args) {
    return new HiveFuncHolderExpr(this.nameUsed, this.holder, args, this.getPosition());
  }

  @Override
  public int getSelfCost() {
    return FunctionTemplate.FunctionCostCategory.getDefault().getValue();
  }

}
