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

import java.util.List;

import org.apache.drill.common.expression.fn.FuncHolder;
import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

/**
 * Represents an actual call (a reference) to a declared function.
 * Holds the name used (functions can have multiple aliases), the
 * function declaration, and the actual argument expressions used
 * in this call. This might be better named
 * <code>FunctionCallExpression</code> as it represents a use
 * of a function. Subclasses hold references to the declaration
 * depending on the type (Drill, Hive) of the function.
 */
public abstract class FunctionHolderExpression extends LogicalExpressionBase {
  public final ImmutableList<LogicalExpression> args;
  public final String nameUsed;

  /**
   * Identifies the output field. References that field in the
   * generated classes.
   */
  private FieldReference fieldReference;

  public FunctionHolderExpression(String nameUsed, ExpressionPosition pos, List<LogicalExpression> args) {
    super(pos);
    this.nameUsed = nameUsed;
    if (args == null) {
      this.args = ImmutableList.of();
    } else {
      this.args = ImmutableList.copyOf(args);
    }
  }

  @Override
  public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
    return visitor.visitFunctionHolderExpression(this, value);
  }

  /**
   * A function can have multiple names, it returns the function name used in the query.
   * @return The function name used in the query.
   */
  public String getName() {
    return nameUsed;
  }

  /**
   * constant input expected for i'th argument?
   * @param i
   * @return True if a constant input is expected for the i'th argument. False otherwise.
   */
  public abstract boolean argConstantOnly(int i);

  /**
   * @return aggregating function or not
   */
  public abstract boolean isAggregating();

  /**
   * Is the function output non-deterministic?
   */
  public abstract boolean isRandom();

  /**
   * @return a copy of FunctionHolderExpression, with passed in argument list.
   */
  public abstract FunctionHolderExpression copy(List<LogicalExpression> args);

  /** Return the underlying function implementation holder. That is,
   * returns the function declaration. */
  public abstract FuncHolder getHolder();

  public FieldReference getFieldReference() {
    return fieldReference;
  }

  /**
   * Set the FieldReference to be used during generating code.
   *
   * @param fieldReference FieldReference to set.
   */
  public void setFieldReference(FieldReference fieldReference) {
    this.fieldReference = fieldReference;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder()
        .append("[").append(getClass().getSimpleName())
        .append(", ")
        .append(nameUsed).append("(");
    boolean first = true;
    for (LogicalExpression arg : args) {
      if (!first) {
        buf.append(", ");
      }
      first = false;
      buf.append(arg.toString());
    }
    return buf.append("]").toString();
  }
}
