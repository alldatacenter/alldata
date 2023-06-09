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
package org.apache.drill.exec.expr.fn.output;

import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.fn.FunctionAttributes;
import org.apache.drill.exec.expr.fn.FunctionUtils;

import java.util.List;

/**
 * Return type calculation implementation for functions with return type set as
 * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#PAD}.
 */
public class PadReturnTypeInference implements ReturnTypeInference {

  public static final PadReturnTypeInference INSTANCE = new PadReturnTypeInference();

  /**
   * Defines function return type and sets precision if it pad length parameter is int expression.
   * If pad length is less than zero, return type precision is 0.
   *
   * @param logicalExpressions logical expressions
   * @param attributes function attributes
   * @return return type
   */
  @Override
  public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
    TypeProtos.MajorType.Builder builder = TypeProtos.MajorType.newBuilder()
        .setMinorType(attributes.getReturnValue().getType().getMinorType())
        .setMode(FunctionUtils.getReturnTypeDataMode(logicalExpressions, attributes));

    if (logicalExpressions.get(1).iterator().hasNext() &&
        logicalExpressions.get(1).iterator().next() instanceof ValueExpressions.IntExpression) {
      int precision = ((ValueExpressions.IntExpression) logicalExpressions.get(1).iterator().next()).getInt();
      // if pad length is less than zero, output length is 0
      builder.setPrecision(Math.max(precision, 0));
    }
    return builder.build();
  }
}
