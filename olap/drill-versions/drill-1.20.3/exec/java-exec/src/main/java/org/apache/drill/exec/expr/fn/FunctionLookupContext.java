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
import org.apache.drill.exec.resolver.FunctionResolver;

public interface FunctionLookupContext {

  /**
   * Method returns the materialized drill function corresponding to the functioncall that
   * is passed in
   *
   * @param functionResolver - Type of resolver to use.
   *                           Specifies if the arguments should match exactly or can use implicit cast
   * @param functionCall - Specifies function name and type of arguments
   * @return DrillFuncHolder
   */
  public DrillFuncHolder findDrillFunction(FunctionResolver functionResolver, FunctionCall functionCall);

  /**
   * Find function implementation for given <code>functionCall</code> in non-Drill function registries such as Hive UDF
   * registry.
   *
   * Note: Order of searching is same as order of {@link org.apache.drill.exec.expr.fn.PluggableFunctionRegistry}
   * implementations found on classpath.
   *
   * @param functionCall - Specifies function name and type of arguments
   * @return AbstractFuncHolder
   */
  public AbstractFuncHolder findNonDrillFunction(FunctionCall functionCall);
}
