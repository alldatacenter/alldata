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
package org.apache.drill.common.expression;

import java.util.List;

import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;

public class BooleanOperator extends FunctionCall {

  public BooleanOperator(String name, List<LogicalExpression> args, ExpressionPosition pos) {
    super(name, args, pos);
  }

  @Override
  public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
    return visitor.visitBooleanOperator(this, value);
  }

  @Override
  public MajorType getMajorType() {
    // If any of argument of a boolean "and"/"or" is nullable, the result is nullable bit.
    // Otherwise, it's non-nullable bit.
    for (LogicalExpression e : args) {
      if (e.getMajorType().getMode() == DataMode.OPTIONAL) {
        return Types.OPTIONAL_BIT;
      }
    }
    return Types.REQUIRED_BIT;
  }

  @Override
  public int getCumulativeCost() {
    // return the average cost of operands for a boolean "and" | "or"
    int cost = this.getSelfCost();

    int i = 0;
    for (LogicalExpression e : this) {
      cost += e.getCumulativeCost();
      i++;
    }

    return cost / i;
  }
}
