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

package com.netease.arctic.flink.util;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.runtime.taskexecutor.GlobalAggregateManager;
import org.apache.flink.runtime.taskexecutor.rpc.RpcGlobalAggregateManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An util class of global aggregate manager that simulates action as {@link RpcGlobalAggregateManager}
 * in the jobMaster.
 */
public class TestGlobalAggregateManager implements GlobalAggregateManager {
  private Map<String, Object> accumulators = new HashMap<>();

  @Override
  public <IN, ACC, OUT> OUT updateGlobalAggregate(
      String aggregateName,
      Object aggregand,
      AggregateFunction<IN, ACC, OUT> aggregateFunction) throws IOException {

    Object accumulator = accumulators.get(aggregateName);
    if (null == accumulator) {
      accumulator = aggregateFunction.createAccumulator();
    }

    accumulator = aggregateFunction.add((IN) aggregand, (ACC) accumulator);
    accumulators.put(aggregateName, accumulator);
    return aggregateFunction.getResult((ACC) accumulator);
  }
}
