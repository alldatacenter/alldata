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
 * Indicates double expression predicate implementations.
 */
public abstract class DoubleExpressionPredicate implements FilterExpression {

  private final FilterExpression right;
  private final Operator operator;
  private final FilterExpression left;

  protected DoubleExpressionPredicate(FilterExpression right, Operator operator, FilterExpression left) {
    this.right = right;
    this.operator = operator;
    this.left = left;
  }

  public FilterExpression right() {
    return right;
  }

  public FilterExpression left() {
    return left;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DoubleExpressionPredicate.class.getSimpleName() + "[", "]")
      .add("right=" + right)
      .add("operator=" + operator)
      .add("left=" + left)
      .toString();
  }

  /**
   * Indicates {@link FilterExpression.Operator#AND} operator expression:
   * storagePlugin = 'dfs' and workspace = 'tmp'.
   */
  public static class And extends DoubleExpressionPredicate {

    public And(FilterExpression right, FilterExpression left) {
      super(right, Operator.AND, left);
    }

    @Override
    public <V> V accept(Visitor<V> visitor) {
      return visitor.visit(this);
    }
  }

  /**
   * Indicates {@link FilterExpression.Operator#OR} operator expression:
   * storagePlugin = 'dfs' or storagePlugin = 's3'.
   */
  public static class Or extends DoubleExpressionPredicate {

    public Or(FilterExpression right, FilterExpression left) {
      super(right, Operator.OR, left);
    }

    @Override
    public <V> V accept(Visitor<V> visitor) {
      return visitor.visit(this);
    }
  }
}
