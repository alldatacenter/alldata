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
package org.apache.drill.exec.resolver;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;

import java.util.List;

public class ExactFunctionResolver implements FunctionResolver {

  /*
   * This function resolves the input call to a func holder only if all
   * the input argument types match exactly with the func holder arguments. This is used when we
   * are trying to inject an implicit cast and do not want to inject another implicit
   * cast
   */
  @Override
  public DrillFuncHolder getBestMatch(List<DrillFuncHolder> methods, FunctionCall call) {

    int currCost;

    final List<TypeProtos.MajorType> argumentTypes = Lists.newArrayList();
    for (LogicalExpression expression : call.args()) {
      argumentTypes.add(expression.getMajorType());
    }

    for (DrillFuncHolder h : methods) {
      currCost = TypeCastRules.getCost(argumentTypes, h);
      // Return if we found a function that has an exact match with the input arguments
      if (currCost == 0) {
        return h;
      }
    }
    // No match found
    return null;
  }
}
