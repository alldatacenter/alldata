/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.predicate.expressions;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.api.predicate.operators.Operator;

/**
 * Base class for expressions.
 */
public abstract class AbstractExpression<T> implements Expression<T> {

  /**
   * The operator.
   */
  private final Operator m_op;

  /**
   * The left operand.
   * */
  private T m_left = null;

  /**
   * The right operand.
   */
  private T m_right = null;

  /**
   * Constructor.
   *
   * @param op  the expressions operator
   */
  protected AbstractExpression(Operator op) {
    m_op = op;
  }

  @Override
  public void setLeftOperand(T left) {
    m_left = left;
  }

  @Override
  public void setRightOperand(T right) {
    m_right = right;
  }

  @Override
  public T getLeftOperand() {
    return m_left;
  }

  @Override
  public T getRightOperand() {
    return m_right;
  }

  @Override
  public Operator getOperator() {
    return m_op;
  }

  @Override
  public List<Expression> merge(Expression left, Expression right, int precedence) {
    return defaultMerge(left, right);
  }

  /**
   * Base merge implementation.
   * No merge is done, simply returns the left expression, this and the right expression.
   *
   * @param left   the expression to the left of this expression
   * @param right  the expression to the right of this expression
   *
   * @return a list containing the un-merged left expression, this and right expression
   */
  protected List<Expression> defaultMerge(Expression left, Expression right) {
    List<Expression> listExpressions = new ArrayList<>();
    if (left != null) {
      listExpressions.add(left);
    }
    listExpressions.add(this);
    if (right != null) {
      listExpressions.add(right);
    }

    return listExpressions;
  }
}
