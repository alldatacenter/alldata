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

import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.api.predicate.operators.LogicalOperator;
import org.apache.ambari.server.controller.spi.Predicate;

/**
 * Logical expression implementation.
 * Always a binary expression that consists of a logical operator and
 * expressions for the left and right operands.
 */
public class LogicalExpression extends AbstractExpression<Expression> {

  /**
   * Constructor.
   *
   * @param op  the logical operator of the expression
   */
  public LogicalExpression(LogicalOperator op) {
    super(op);
  }


  @Override
  public Predicate toPredicate() throws InvalidQueryException {
    return ((LogicalOperator) getOperator()).
        toPredicate(getLeftOperand().toPredicate(), getRightOperand().toPredicate());
  }

  @Override
  public List<Expression> merge(Expression left, Expression right, int precedence) {
    if (getOperator().getPrecedence() == precedence && getLeftOperand() == null) {
      setLeftOperand(left);
      setRightOperand(right);
      return Collections.singletonList(this);
    } else {
      return defaultMerge(left, right);
    }
  }
}
