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

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.api.predicate.operators.LogicalOperator;
import org.apache.ambari.server.controller.predicate.NotPredicate;
import org.apache.ambari.server.controller.spi.Predicate;

/**
 * A 'NOT' logical expression representation.
 * Negates a corresponding right operand.
 */
public class NotLogicalExpression extends LogicalExpression {
  /**
   * Constructor.
   *
   * @param op  the logical operator
   */
  public NotLogicalExpression(LogicalOperator op) {
    super(op);
  }

  @Override
  public List<Expression> merge(Expression left, Expression right, int precedence) {
    if (getOperator().getPrecedence() == precedence && getRightOperand() == null) {
      List<Expression> listExpressions = new ArrayList<>();
      if (left != null) {
        listExpressions.add(left);
      }
      setRightOperand(right);
      listExpressions.add(this);
      return listExpressions;
    } else {
      // do nothing, already merged
      return defaultMerge(left, right);
    }
  }

  @Override
  public Predicate toPredicate() throws InvalidQueryException {
    return new NotPredicate(getRightOperand().toPredicate());
  }
}
