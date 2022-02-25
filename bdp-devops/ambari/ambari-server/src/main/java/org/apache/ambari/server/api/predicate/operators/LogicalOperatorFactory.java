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

package org.apache.ambari.server.api.predicate.operators;

import org.apache.ambari.server.api.predicate.InvalidQueryException;

/**
 * Factory of Logical Operators.
 */
public class LogicalOperatorFactory {
  /**
   * Creates a logical operator based on the operator token.
   *
   * @param operator      string representation of operator
   * @param ctxPrecedence precedence value of current context
   *
   * @return a logical operator instance
   * @throws InvalidQueryException if the operator string is invalid
   */
  public static LogicalOperator createOperator(String operator, int ctxPrecedence)
      throws InvalidQueryException {
    if ("&".equals(operator)) {
      return new AndOperator(ctxPrecedence);
    } else if ("|".equals(operator)) {
      return new OrOperator(ctxPrecedence);
    } else if ("!".equals(operator)) {
      return new NotOperator(ctxPrecedence);
    } else {
      throw new RuntimeException("Invalid Logical Operator Type: " + operator);
    }
  }
}
