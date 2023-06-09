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

import java.util.List;

import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;

/**
 * An implementing class of FunctionResolver provide their own algorithm to choose a DrillFuncHolder from a given list of
 * candidates, with respect to a given FunctionCall
 */
public interface FunctionResolver {
  /**
   * Creates a placeholder SqlFunction for an invocation of a function with a
   * possibly qualified name. This name must be resolved into either a builtin
   * function or a user-defined function.
   *
   * @param methods   a list of candidates of DrillFuncHolder to be chosen from
   * @param call      a given function call whose DrillFuncHolder is to be determined via this method
   * @return DrillFuncHolder the chosen DrillFuncHolder
   */
  DrillFuncHolder getBestMatch(List<DrillFuncHolder> methods, FunctionCall call);
}
