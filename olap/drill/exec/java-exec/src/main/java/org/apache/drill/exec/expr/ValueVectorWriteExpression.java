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
package org.apache.drill.exec.expr;

import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.record.TypedFieldId;

import java.util.Iterator;

public class ValueVectorWriteExpression implements LogicalExpression {
  private final TypedFieldId fieldId;
  private final LogicalExpression child;
  private final boolean safe;

  public ValueVectorWriteExpression(TypedFieldId fieldId, LogicalExpression child){
    this(fieldId, child, false);
  }

  public ValueVectorWriteExpression(TypedFieldId fieldId, LogicalExpression child, boolean safe){
    this.fieldId = fieldId;
    this.child = child;
    this.safe = safe;
  }

  public TypedFieldId getFieldId() {
    return fieldId;
  }

  @Override
  public MajorType getMajorType() {
    return Types.NULL;
  }

  public boolean isSafe() {
    return safe;
  }

  @Override
  public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
    if (visitor instanceof AbstractExecExprVisitor) {
      AbstractExecExprVisitor<T, V, E> abstractExecExprVisitor = (AbstractExecExprVisitor<T, V, E>) visitor;
      return abstractExecExprVisitor.visitValueVectorWriteExpression(this, value);
    }
    return visitor.visitUnknown(this, value);
  }

  @Override
  public ExpressionPosition getPosition() {
    return ExpressionPosition.UNKNOWN;
  }

  public LogicalExpression getChild() {
    return child;
  }

  @Override
  public Iterator<LogicalExpression> iterator() {
    return Iterators.singletonIterator(child);
  }

  @Override
  public int getSelfCost() {
    return 0;  // TODO
  }

  @Override
  public int getCumulativeCost() {
    return 0; // TODO
  }
}
