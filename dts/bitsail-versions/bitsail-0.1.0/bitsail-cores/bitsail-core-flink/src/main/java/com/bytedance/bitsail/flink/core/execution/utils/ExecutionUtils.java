/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.flink.core.execution.utils;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.flink.core.option.FlinkCommonOptions;

import org.apache.commons.lang.StringUtils;
import org.apache.flink.streaming.api.datastream.DataStream;

/**
 * Created 2022/6/15
 */
public class ExecutionUtils {

  public static <T> DataStream<T> addExecutionPartitioner(DataStream<T> previous,
                                                          BitSailConfiguration commonConfiguration) {
    String executionPartitioner = commonConfiguration.get(FlinkCommonOptions.EXECUTION_PARTITIONER);
    if (StringUtils.isEmpty(executionPartitioner)) {
      return previous;
    }
    if (StringUtils.equalsIgnoreCase(executionPartitioner, "RESCALE")) {
      return previous.rescale();
    }
    if (StringUtils.equalsIgnoreCase(executionPartitioner, "REBALANCE")) {
      return previous.rebalance();
    }
    return previous;
  }
}
