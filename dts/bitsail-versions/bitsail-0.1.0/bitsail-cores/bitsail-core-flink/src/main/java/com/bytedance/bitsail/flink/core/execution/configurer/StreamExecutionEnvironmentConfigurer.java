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

package com.bytedance.bitsail.flink.core.execution.configurer;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.flink.core.FlinkJobMode;
import com.bytedance.bitsail.flink.core.execution.FlinkExecutionEnviron;
import com.bytedance.bitsail.flink.core.option.FlinkCommonOptions;
import com.bytedance.bitsail.flink.core.parallelism.FlinkParallelismAdvisor;
import com.bytedance.bitsail.flink.core.runtime.restart.FailureRateRestartStrategyBuilder;
import com.bytedance.bitsail.flink.core.runtime.restart.FixedDelayRestartStrategyBuilder;
import com.bytedance.bitsail.flink.core.runtime.restart.FlinkRestartStrategyBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.ExecutionMode;
import org.apache.flink.api.common.restartstrategy.RestartStrategies.RestartStrategyConfiguration;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class StreamExecutionEnvironmentConfigurer {
  private static final Logger LOG = LoggerFactory.getLogger(StreamExecutionEnvironmentConfigurer.class);

  private final FlinkJobMode flinkJobMode;
  private final BitSailConfiguration commonConfiguration;
  private final FlinkExecutionEnviron executionEnviron;
  private final FlinkParallelismAdvisor parallelismAdvisor;

  private final ExecutionMode executionMode;
  private final StreamExecutionEnvironment executionEnvironment;

  public StreamExecutionEnvironmentConfigurer(FlinkJobMode flinkJobMode,
                                              FlinkExecutionEnviron executionEnviron,
                                              BitSailConfiguration commonConfiguration) {
    this.flinkJobMode = flinkJobMode;
    this.executionEnviron = executionEnviron;
    this.commonConfiguration = commonConfiguration;
    this.parallelismAdvisor = executionEnviron.getParallelismAdvisor();

    this.executionMode = ExecutionMode.valueOf(StringUtils
        .upperCase(commonConfiguration.get(FlinkCommonOptions.EXECUTION_MODE)));
    this.executionEnvironment = executionEnviron.getExecutionEnvironment();
  }

  /**
   * get checkpoint config from jobConf or execution environment
   */
  private static int getDefaultCheckpointTolerableFailureNumber(Configuration configuration,
                                                                BitSailConfiguration commonConfiguration) {
    if (commonConfiguration.fieldExists(CommonOptions.CheckPointOptions.CHECKPOINT_TOLERABLE_FAILURE_NUMBER_KEY)) {
      return commonConfiguration.get(CommonOptions.CheckPointOptions.CHECKPOINT_TOLERABLE_FAILURE_NUMBER_KEY);
    }
    return -1;
  }

  public void prepareExecutionEnvironment() {
    configureCheckpoint();
    if (FlinkJobMode.STREAMING.equals(flinkJobMode)) {
      configureEnvironmentParallelism();
      configureTimeCharacteristic();
    }
    configureRestartStrategy();
    executionEnviron.setExecutionEnvironment(executionEnvironment);
  }

  private void configureRestartStrategy() {
    FlinkRestartStrategyBuilder builder;
    switch (flinkJobMode.getRestartStrategy()) {
      case FIXED_DELAY_RESTART:
        builder = new FixedDelayRestartStrategyBuilder();
        break;
      case FAILURE_RATE_RESTART:
        builder = new FailureRateRestartStrategyBuilder();
        break;
      default:
        return;
    }

    builder.setCommonConf(commonConfiguration);
    builder.setParallelismAdvisor(executionEnviron.getParallelismAdvisor());
    builder.setExecutionMode(executionMode);

    RestartStrategyConfiguration restartStrategyConfiguration = builder.buildRestartStrategyConf();
    if (Objects.nonNull(restartStrategyConfiguration)) {
      executionEnvironment.setRestartStrategy(restartStrategyConfiguration);
    }
  }

  private void configureCheckpoint() {
    boolean checkpointEnable = commonConfiguration.get(CommonOptions.CheckPointOptions.CHECKPOINT_ENABLE);
    if (checkpointEnable || FlinkJobMode.STREAMING.equals(flinkJobMode)) {
      long checkpointInterval = commonConfiguration.get(CommonOptions.CheckPointOptions.CHECKPOINT_INTERVAL);
      long checkpointTimeout = commonConfiguration.get(CommonOptions.CheckPointOptions.CHECKPOINT_TIMEOUT);
      int tolerableFailureNumber = getDefaultCheckpointTolerableFailureNumber(executionEnviron.getFlinkConfiguration(), commonConfiguration);
      LOG.info("Checkpoint tolerable failure number: {}.", tolerableFailureNumber);

      executionEnvironment.enableCheckpointing(checkpointInterval);
      if (tolerableFailureNumber > 0) {
        executionEnvironment.getCheckpointConfig().setTolerableCheckpointFailureNumber(tolerableFailureNumber);
      }
      executionEnvironment.getCheckpointConfig().setCheckpointTimeout(checkpointTimeout);
    }
  }

  private void configureEnvironmentParallelism() {
    int globalParallelism = parallelismAdvisor.getGlobalParallelism();
    LOG.info("Global parallelism is {}.", globalParallelism);
    executionEnvironment.setParallelism(globalParallelism);

    int userConfigMaxParallelism = commonConfiguration.getUnNecessaryOption(FlinkCommonOptions.FLINK_MAX_PARALLELISM, -1);
    if (userConfigMaxParallelism > 0) {
      executionEnvironment.setMaxParallelism(userConfigMaxParallelism);
      LOG.info("Global max parallelism is {}.", userConfigMaxParallelism);
    }
  }

  private void configureTimeCharacteristic() {
    executionEnvironment.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
  }
}
