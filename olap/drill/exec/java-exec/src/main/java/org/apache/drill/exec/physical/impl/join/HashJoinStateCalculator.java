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
package org.apache.drill.exec.physical.impl.join;

import javax.annotation.Nullable;

/**
 * A {@link HashJoinStateCalculator} is a piece of code that compute the memory requirements for one of the states
 * in the {@link HashJoinState} enum.
 */
public interface HashJoinStateCalculator<T extends HashJoinStateCalculator<?>> {
  /**
   * Signifies that the current state is complete and returns the next {@link HashJoinStateCalculator}.
   * Returns null in the case where there is no next state.
   * @return The next {@link HashJoinStateCalculator} or null if this was the last state.
   */
  @Nullable
  T next();

  /**
   * The current {@link HashJoinState} corresponding to this calculator.
   * @return
   */
  HashJoinState getState();
}
