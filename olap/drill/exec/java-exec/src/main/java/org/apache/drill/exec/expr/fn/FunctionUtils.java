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
package org.apache.drill.exec.expr.fn;

import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;

import java.util.List;

public class FunctionUtils {

  /**
   * Calculates return type data mode based on give logical expressions.
   * If null handling strategy is internal, returns return value data mode.
   * If null handling strategy is null if null and at least one of the input types are nullable,
   * return nullable data mode.
   *
   * @param logicalExpressions logical expressions
   * @param attributes function attributes
   * @return data mode
   */
  public static TypeProtos.DataMode getReturnTypeDataMode(final List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
    if (attributes.getNullHandling() == FunctionTemplate.NullHandling.NULL_IF_NULL) {
      for (final LogicalExpression logicalExpression : logicalExpressions) {
        if (logicalExpression.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) {
          return TypeProtos.DataMode.OPTIONAL;
        }
      }
    }
    return attributes.getReturnValue().getType().getMode();
  }

}
