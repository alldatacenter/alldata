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
package org.apache.drill.exec.expr.fn.registry;

import org.apache.drill.exec.expr.fn.DrillFuncHolder;

/**
 * Holder class that contains:
 * <ol>
 *   <li>function name</li>
 *   <li>function signature which is string representation of function name and its input parameters</li>
 *   <li>{@link DrillFuncHolder} associated with the function</li>
 * </ol>
 */
public class FunctionHolder {

  private final String name;
  private final String signature;
  private final DrillFuncHolder holder;

  public FunctionHolder(String name, String signature, DrillFuncHolder holder) {
    this.name = name;
    this.signature = signature;
    this.holder = holder;
  }

  public String getName() {
    return name;
  }

  public DrillFuncHolder getHolder() {
    return holder;
  }

  public String getSignature() {
    return signature;
  }

}
