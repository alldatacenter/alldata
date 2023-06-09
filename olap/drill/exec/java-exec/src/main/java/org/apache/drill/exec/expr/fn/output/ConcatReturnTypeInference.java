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
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.fn.FunctionAttributes;
import org.apache.drill.exec.expr.fn.FunctionUtils;

import java.util.List;

/**
 * Return type calculation implementation for functions with return type set as
 * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#CONCAT}.
 */
public class ConcatReturnTypeInference implements ReturnTypeInference {

  public static final ConcatReturnTypeInference INSTANCE = new ConcatReturnTypeInference();

  /**
   * Defines function return type and sets precision if it can be calculated.
   * Return type precision is sum of input types precisions.
   * If at least one input type does not have precision, return type will be without precision.
   * If calculated precision is greater than {@link Types#MAX_VARCHAR_LENGTH},
   * it is replaced with {@link Types#MAX_VARCHAR_LENGTH}.
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

    int totalPrecision = 0;
    for (LogicalExpression expression : logicalExpressions) {
      if (expression.getMajorType().hasPrecision()) {
        totalPrecision += expression.getMajorType().getPrecision();
      } else {
        // if at least one expression has unknown precision, return type without precision
        return builder.build();
      }
    }
    return builder.setPrecision(totalPrecision > Types.MAX_VARCHAR_LENGTH ? Types.MAX_VARCHAR_LENGTH : totalPrecision).build();
  }
}
