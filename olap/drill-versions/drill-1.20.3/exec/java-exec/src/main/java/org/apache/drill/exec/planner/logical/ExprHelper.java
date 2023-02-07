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
package org.apache.drill.exec.planner.logical;

import java.util.List;

import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;

public class ExprHelper {
  private final static String COMPOUND_FAIL_MESSAGE =
      "The Calcite-based logical plan interpreter does not handle complicated expressions. For Order By and Filter";

  public static String getAggregateFieldName(FunctionCall c) {
    List<LogicalExpression> exprs = c.args();
    if (exprs.size() != 1) {
      throw new UnsupportedOperationException(COMPOUND_FAIL_MESSAGE);
    }
    return getFieldName(exprs.iterator().next());
  }

  public static String getFieldName(LogicalExpression e) {
    //if(e instanceof SchemaPath) return ((SchemaPath) e).getPath().toString();
    throw new UnsupportedOperationException(COMPOUND_FAIL_MESSAGE);
  }
}
