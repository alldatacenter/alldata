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
package org.apache.drill.metastore.expressions;

import java.util.StringJoiner;

/**
 * Indicates single expression predicate implementations.
 */
public abstract class SingleExpressionPredicate implements FilterExpression {

  private final FilterExpression expression;
  private final Operator operator;

  protected SingleExpressionPredicate(FilterExpression expression, Operator operator) {
    this.expression = expression;
    this.operator = operator;
  }

  public FilterExpression expression() {
    return expression;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SingleExpressionPredicate.class.getSimpleName() + "[", "]")
      .add("expression=" + expression)
      .add("operator=" + operator)
      .toString();
  }

  /**
   * Indicates {@link FilterExpression.Operator#NOT} operator expression:
   * not(storagePlugin = 'dfs').
   */
  public static class Not extends SingleExpressionPredicate {

    public Not(FilterExpression expression) {
      super(expression, Operator.NOT);
    }

    @Override
    public <V> V accept(Visitor<V> visitor) {
      return visitor.visit(this);
    }
  }
}
