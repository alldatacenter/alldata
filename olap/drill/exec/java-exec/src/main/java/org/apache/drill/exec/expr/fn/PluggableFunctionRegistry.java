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

import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.exec.planner.sql.DrillOperatorTable;

public interface PluggableFunctionRegistry {

  /**
   * Register functions in given operator table. There are two methods to add operators.
   * One is addOperatorWithInference whose added operators will be used
   * when planner.type_inference.enable is set to true;
   * The other is addOperatorWithoutInference whose added operators will be used
   * when planner.type_inference.enable is set to false;
   * @param operatorTable
   */
  public void register(DrillOperatorTable operatorTable);

  /**
   * If exists return the function implementation holder matching the given <code>functionCall</code> expression,
   * otherwise null.
   *
   * @param functionCall
   * @return
   */
  public AbstractFuncHolder getFunction(FunctionCall functionCall);
}
