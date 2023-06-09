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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.visitors.ExpressionValidationError;
import org.apache.drill.common.types.TypeProtos.MajorType;

import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.collect.Range;
import org.slf4j.Logger;

public class ErrorCollectorImpl implements ErrorCollector {
  private final List<ExpressionValidationError> errors = new ArrayList<>();

  private String addExpr(ExpressionPosition expr, String message) {
    return String.format(
        "Error in expression at index %d.  Error: %s. Full expression: %s.",
        expr.getCharIndex(), message, expr.getExpression());
  }

  @Override
  public void addGeneralError(ExpressionPosition expr, String s) {
    errors.add(new ExpressionValidationError(addExpr(expr, s)));
  }

  @Override
  public void addUnexpectedArgumentType(ExpressionPosition expr, String name,
      MajorType actual, MajorType[] expected, int argumentIndex) {
    errors.add(
        new ExpressionValidationError(
            addExpr(expr, String.format(
                "Unexpected argument type. Index :%d Name: %s, Type: %s, Expected type(s): %s",
                argumentIndex, name, actual, Arrays.toString(expected)
            ))
        )
    );
  }

  @Override
  public void addUnexpectedArgumentCount(ExpressionPosition expr, int actual, Range<Integer> expected) {
    errors.add(new ExpressionValidationError(
            addExpr(expr, String.format(
                "Unexpected argument count. Actual argument count: %d, Expected range: %s",
                actual, expected))
    ));
  }

  @Override
  public void addUnexpectedArgumentCount(ExpressionPosition expr, int actual, int expected) {
    errors.add(new ExpressionValidationError(
        addExpr(expr, String.format(
            "Unexpected argument count. Actual argument count: %d, Expected count: %d",
            actual, expected))
    ));
  }

  @Override
  public void addNonNumericType(ExpressionPosition expr, MajorType actual) {
    errors.add(new ExpressionValidationError(
        addExpr(expr, String.format(
            "Unexpected numeric type. Actual type: %s", actual))
    ));
  }

  @Override
  public void addUnexpectedType(ExpressionPosition expr, int index, MajorType actual) {
    errors.add(new ExpressionValidationError(
        addExpr(expr, String.format(
            "Unexpected argument type. Actual type: %s, Index: %d", actual, index))
    ));
  }

  @Override
  public void addExpectedConstantValue(ExpressionPosition expr, int actual, String s) {
    errors.add(new ExpressionValidationError(
        addExpr(expr, String.format(
            "Unexpected constant value. Name: %s, Actual: %s", s, actual))
    ));
  }

  @Override
  public boolean hasErrors() {
      return !errors.isEmpty();
  }

  @Override
  public int getErrorCount() {
    return errors.size();
  }

  @Override
  public String toErrorString() {
    return "\n" + Joiner.on("\n").join(errors);
  }

  @Override
  public String toString() {
    return toErrorString();
  }

  @Override
  public void reportErrors(Logger logger) {
    if (!hasErrors()) {
      return;
    }
    throw UserException.internalError(null)
      .message("Failure while materializing expression.")
      .addContext("Errors", toErrorString())
      .build(logger);
  }
}
