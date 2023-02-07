/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.physical.config;

import org.apache.drill.exec.physical.base.AbstractSingle;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;

public class IteratorValidator extends AbstractSingle{

  public static final String OPERATOR_TYPE = "ITERATOR_VALIDATOR";

  /* isRepeatable flag will be set to true if this validator is created by a Repeatable pipeline.
   * In a repeatable pipeline some state transitions are valid i.e downstream operator
   * can call the upstream operator even after receiving NONE.
   */
  public final boolean isRepeatable;

  public IteratorValidator(PhysicalOperator child, boolean repeatable) {
    super(child);
    setCost(child.getCost());
    this.isRepeatable = repeatable;
  }

  public IteratorValidator(PhysicalOperator child) {
    this(child, false);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitIteratorValidator(this, value);
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new IteratorValidator(child, isRepeatable);
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }
}
