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

import java.util.List;

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.api.predicate.operators.Operator;
import org.apache.ambari.server.controller.spi.Predicate;

/**
 * Expression representation.
 * There are two types of expressions, relational and logical.
 * Each expression has an operator and either 2 operands for binary
 * expressions or 1 operand for unary expressions.
 */
public interface Expression<T> {

  /**
   * Merge expression with surrounding expressions.
   *
   * @param left        the preceding expression
   * @param right       the following expression
   * @param precedence  the precedence level being merged.  Only expressions at this precedence level
   *                    should be merged. Others should simply return the left expression, themselves and
   *                    the right expression in that order.
   *
   * @return a list of expressions after merging.  Do not return any null elements.
   */
  List<Expression> merge(Expression left, Expression right, int precedence);


  /**
   * Get the predicate representation of the expression.
   * @return a predicate instance for the expression
   */
  Predicate toPredicate() throws InvalidQueryException;

  /**
   * Set the expressions left operand.
   *
   * @param left  the left operand
   */
  void setLeftOperand(T left);

  /**
   * Set the expressions right operand.
   *
   * @param right  the right operand
   */
  void setRightOperand(T right);

  /**
   * Get the left operand expression.
   *
   * @return the left operand
   */
  T getLeftOperand();

  /**
   * Get the right operand expression.
   *
   * @return the right operand.
   */
  T getRightOperand();

  /**
   * Get the expression operator.
   *
   * @return the logical operator for the expression
   */
  Operator getOperator();
}
