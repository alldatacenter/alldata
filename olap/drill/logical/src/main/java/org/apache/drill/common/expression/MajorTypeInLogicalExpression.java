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
package org.apache.drill.common.expression;

import java.util.Iterator;

import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.common.types.TypeProtos;

/**
 * MajorTypeInLogicalExpression is a LogicalExpression, which wraps a given @{TypeProtos.MajorType}
 */
public class MajorTypeInLogicalExpression implements LogicalExpression {
  private TypeProtos.MajorType majorType;

  public MajorTypeInLogicalExpression(TypeProtos.MajorType majorType) {
    this.majorType = majorType;
  }

  @Override
  public TypeProtos.MajorType getMajorType() {
    return majorType;
  }

  @Override
  public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
    throw new UnsupportedOperationException();
  }

  @Override
  public ExpressionPosition getPosition() {
    throw new UnsupportedOperationException();
  }

  public int getSelfCost() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getCumulativeCost() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<LogicalExpression> iterator() {
    throw new UnsupportedOperationException();
  }
}