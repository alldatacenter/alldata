/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.netease.arctic.optimizer;

import com.netease.arctic.optimizer.operator.BaseToucher;

import java.util.Map;

/**
 * StatefulOptimizer is a kind of {@link Optimizer} with state.
 * Ams will receive the state reported from {@link BaseToucher}, and set the state to StatefulOptimizer
 * by invoke {@link StatefulOptimizer#updateState(Map)}.
 */
public interface StatefulOptimizer extends Optimizer {
  /**
   * Get the state of Optimizer.
   *
   * @return map of state
   */
  Map<String, String> getState();

  /**
   * Update the state of Optimizer.
   *
   * @param state -
   */
  void updateState(Map<String, String> state);

}
