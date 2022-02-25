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
 * Factory of relational operators.
 */
public class RelationalOperatorFactory {
  /**
   * Create a relational operator based on the string representation
   * of the operator.
   *
   * @param operator  the string representation of the operator
   *
   * @return relational operator for the given string
   * @throws InvalidQueryException if an invalid operator is passed in
   */
  public static RelationalOperator createOperator(String operator) throws InvalidQueryException {
    if ("!=".equals(operator)) {
      return new NotEqualsOperator();
    } else if ("=".equals(operator)) {
      return new EqualsOperator();
    } else if ("<=".equals(operator)) {
      return new LessEqualsOperator();
    } else if ("<".equals(operator)) {
      return new LessOperator();
    } else if (">=".equals(operator)) {
      return new GreaterEqualsOperator();
    } else if (">".equals(operator)) {
      return new GreaterOperator();
    } else if (".in(".equals(operator)) {
      return new InOperator();
    } else if (".isEmpty(".equals(operator)) {
      return new IsEmptyOperator();
    } else if (".matches(".equals(operator)) {
      return new FilterOperator();
    } else {
      throw new RuntimeException("Invalid Operator Type: " + operator);
    }
  }
}
