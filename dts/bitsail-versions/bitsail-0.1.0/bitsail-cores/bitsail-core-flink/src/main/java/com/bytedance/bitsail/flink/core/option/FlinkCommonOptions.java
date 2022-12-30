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

package com.bytedance.bitsail.flink.core.option;

import com.bytedance.bitsail.common.option.ConfigOption;

import static com.bytedance.bitsail.common.option.CommonOptions.COMMON_PREFIX;
import static com.bytedance.bitsail.common.option.ConfigOptions.key;

public interface FlinkCommonOptions {

  /**
   * Min parallelism
   */
  ConfigOption<Integer> FLINK_MIN_PARALLELISM =
      key(COMMON_PREFIX + "min_parallelism")
          .defaultValue(1);

  /**
   * Max parallelism
   */
  ConfigOption<Integer> FLINK_MAX_PARALLELISM =
      key(COMMON_PREFIX + "max_parallelism")
          .defaultValue(512);

  /**
   * Parallelism ratio
   */
  ConfigOption<Float> FLINK_PARALLELISM_RATIO =
      key(COMMON_PREFIX + "parallelism_ratio")
          .defaultValue(1.0f);

  ConfigOption<Boolean> FLINK_PARALLELISM_CHAIN =
      key(COMMON_PREFIX + "parallelism_chain")
          .defaultValue(false);

  /**
   * strategy for computing parallelism of union operator
   */
  ConfigOption<String> FLINK_UNION_PARALLELISM_STRATEGY =
      key(COMMON_PREFIX + "flink_union_parallelism_strategy")
          .defaultValue("MAX");

  ConfigOption<String>
      FLINK_TASK_FAILOVER_STRATEGY =
      key(COMMON_PREFIX + "flink_task_failover_strategy")
          .defaultValue("full");

  ConfigOption<Integer> FLINK_REGION_FAILOVER_ATTEMPTS_NUM =
      key(COMMON_PREFIX + "flink_region_failover_attempts_num")
          .defaultValue(-1);

  ConfigOption<Integer> FLINK_RESTART_DELAY =
      key(COMMON_PREFIX + "flink_restart_delay")
          .defaultValue(10);

  ConfigOption<Double> FLINK_RESTART_ATTEMPTS_RATIO =
      key(COMMON_PREFIX + "flink_restart_attempts_ratio")
          .defaultValue(1.5);

  ConfigOption<String> EXECUTION_MODE =
      key(COMMON_PREFIX + "execution_mode")
          .defaultValue("pipelined");

  ConfigOption<String> EXECUTION_PARTITIONER =
      key(COMMON_PREFIX + "execution_partitioner")
          .noDefaultValue(String.class);
}
