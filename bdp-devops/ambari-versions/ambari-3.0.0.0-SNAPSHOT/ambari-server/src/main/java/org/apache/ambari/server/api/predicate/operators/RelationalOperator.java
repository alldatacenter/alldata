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
import org.apache.ambari.server.controller.spi.Predicate;

/**
 * Relational operator external representation.
 */
public interface RelationalOperator extends Operator {
  /**
   * Create a predicate for this relational operator.
   *
   * @param prop  left operand
   * @param val   right operand
   * @return  a predicate instance for this operator.
   * @throws  InvalidQueryException if unable to build the predicate because of invalid operands
   */
  Predicate toPredicate(String prop, String val) throws InvalidQueryException;
}
