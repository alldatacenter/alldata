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
 * Base operator implementation.
 */
public abstract class AbstractOperator implements Operator {
  /**
   * The precedence value for the current context.
   */
  private final int m_ctxPrecedence;


  /**
   * Constructor.
   *
   * @param ctxPrecedence  the context precedence value
   */
  protected AbstractOperator(int ctxPrecedence) {
    m_ctxPrecedence = ctxPrecedence;
  }

  /**
   * Return the base precedence for this operator.
   * This is the value that is specific to the operator
   * type and doesn't take context into account.
   *
   * @return the base precedence for this operator type
   */
  public int getBasePrecedence() {
    // this value is used for all relational operators
    // logical operators override this value
    return -1;
  }

  @Override
  public int getPrecedence() {
    return getBasePrecedence() + m_ctxPrecedence;
  }

  @Override
  public String toString() {
    return getName();
  }

  /**
   * Get the name of the operator.
   *
   * @return the operator name
   */
  public abstract String getName();
}
