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

import org.apache.drill.common.types.TypeProtos.MajorType;

import org.apache.drill.shaded.guava.com.google.common.collect.Range;
import org.slf4j.Logger;

public interface ErrorCollector {

  public void addGeneralError(ExpressionPosition expr, String s);

  public void addUnexpectedArgumentType(ExpressionPosition expr, String name,
      MajorType actual, MajorType[] expected, int argumentIndex);

  public void addUnexpectedArgumentCount(ExpressionPosition expr,
      int actual,  Range<Integer> expected);

  public void addUnexpectedArgumentCount(ExpressionPosition expr,
      int actual, int expected);

  public void addNonNumericType(ExpressionPosition expr, MajorType actual);

  public void addUnexpectedType(ExpressionPosition expr, int index, MajorType actual);

  public void addExpectedConstantValue(ExpressionPosition expr, int actual, String s);

  boolean hasErrors();

  public int getErrorCount();

  String toErrorString();

  /**
   * Checks for errors and throws a user exception if any are found.
   * The caller thus need not implement its own error checking; just
   * call this method.
   *
   * @param logger
   */
  void reportErrors(Logger logger);
}
