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

/**
 * Operator representation.
 */
public interface Operator {

  /**
   * Operator types.
   */
  enum TYPE {
    LESS,
    LESS_EQUAL,
    GREATER,
    GREATER_EQUAL,
    EQUAL,
    NOT_EQUAL,
    AND,
    OR,
    NOT,
    IN,
    IS_EMPTY,
    FILTER
  }

  /**
   * The highest base operator precedence level.
   */
  int MAX_OP_PRECEDENCE = 3;

  /**
   * Get the operator type.
   *
   * @return the operator type
   */
  TYPE getType();

  /**
   * Obtain the precedence of the operator.
   * This value is calculated based on the operators base precedence and the context of the
   * surrounding expressions.  Higher precedence values have higher precedence.
   *
   * @return  the precedence of this operator in it's current context
   */
  int getPrecedence();
}
