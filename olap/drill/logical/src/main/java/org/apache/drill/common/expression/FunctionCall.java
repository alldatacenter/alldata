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
import java.util.List;

import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.common.types.TypeProtos.MajorType;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class FunctionCall extends LogicalExpressionBase implements Iterable<LogicalExpression> {

  // See FunctionNames for a list of well-known built-in functions
  // often used in filter push-down rules

  private final String name;
  protected final ImmutableList<LogicalExpression> args;
  private final ExpressionPosition pos;

  public FunctionCall(String name, List<LogicalExpression> args, ExpressionPosition pos) {
    super(pos);
    this.name = name;

    if (args == null) {
      args = Lists.newArrayList();
    }

    if (!(args instanceof ImmutableList)) {
      args = ImmutableList.copyOf(args);
    }
    this.args = (ImmutableList<LogicalExpression>) args;
    this.pos = pos;
  }

  public String getName() { return name; }

  @Override
  public ExpressionPosition getPosition() { return pos; }

  public List<LogicalExpression> args() { return args; }
  public int argCount() { return args.size(); }
  public LogicalExpression arg(int i) { return args.get(i); }

  @Override
  public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
    return visitor.visitFunctionCall(this, value);
  }

  @Override
  public Iterator<LogicalExpression> iterator() {
    return args.iterator();
  }

  @Override
  public MajorType getMajorType() {
    throw new UnsupportedOperationException(
      "No return type as FunctionCall is not a materialized expression");
  }

  @Override
  public String toString() {
    final int maxLen = 10;
    return "FunctionCall [func=" + name + ", args="
        + (args != null ? args.subList(0, Math.min(args.size(), maxLen)) : null) + ", pos=" + pos + "]";
  }
}
