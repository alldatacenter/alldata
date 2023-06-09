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

import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.common.types.TypeProtos;

import java.util.Collections;
import java.util.Iterator;

public class TypedFieldExpr extends LogicalExpressionBase {
  private final TypeProtos.MajorType type;
  private final SchemaPath path;

  public TypedFieldExpr(SchemaPath path, TypeProtos.MajorType type) {
    super(path.getPosition());
    this.path = path;
    this.type = type;
  }

  @Override
  public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
    return visitor.visitTypedFieldExpr(this, value);
  }

  @Override
  public Iterator<LogicalExpression> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public TypeProtos.MajorType getMajorType() {
    return type;
  }

  @Override
  public String toString() {
    return path.getRootSegment().getPath() + "(" + type.getMinorType() + "_" + type.getMode() +")";
  }

  public SchemaPath getPath() {
    return path;
  }
}
