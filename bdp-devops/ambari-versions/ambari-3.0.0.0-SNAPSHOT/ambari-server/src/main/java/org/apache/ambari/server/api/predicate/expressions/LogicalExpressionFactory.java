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

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.api.predicate.operators.LogicalOperator;

/**
 * Factory of logical expression instances.
 */
public class LogicalExpressionFactory {
  /**
   * Create a logical expression instance.
   *
   * @param op  the logical operator
   *
   * @return a new logical expression instance
   * @throws InvalidQueryException
   */
  public static LogicalExpression createLogicalExpression(LogicalOperator op) throws InvalidQueryException {
    switch (op.getType()) {
      case AND:
      case OR:
        return new LogicalExpression(op);
      case NOT :
        return new NotLogicalExpression(op);
      default:
        throw new RuntimeException("An invalid logical operator type was encountered: " + op);
    }
  }
}
