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

import java.util.Collections;
import java.util.Iterator;

import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.common.types.TypeProtos.MajorType;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;

public class CastExpression extends LogicalExpressionBase implements Iterable<LogicalExpression>{

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CastExpression.class);

  private final LogicalExpression input;
  private final MajorType type;

  public CastExpression(LogicalExpression input, MajorType type, ExpressionPosition pos) {
    super(pos);
    this.input = input;
    this.type = checkNotNull(type, "Major type cannot be null");
  }

  @Override
  public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
    return visitor.visitCastExpression(this, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof CastExpression)) {
      return false;
    }

    CastExpression other = (CastExpression) obj;

    return this.input.equals(other.input) && (this.getMajorType().hasMinorType() == other.getMajorType().hasMinorType());
  }

  @Override
  public int hashCode() {
    //for now we ignore type's other attributes
    return (input.hashCode() << 8) + (0x00ff & type.getMinorType().getNumber());
  }

  @Override
  public Iterator<LogicalExpression> iterator() {
    return Collections.singleton(input).iterator();
  }

  public LogicalExpression getInput() {
    return input;
  }

  @Override
  public MajorType getMajorType() {
    return type;
  }

  @Override
  public String toString() {
    return "CastExpression [input=" + input + ", type=" + type + "]";
  }



}
