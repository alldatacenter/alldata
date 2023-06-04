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

package com.bytedance.bitsail.flink.core.runtime.restart;

import com.bytedance.bitsail.common.option.CommonOptions;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class FailureRateRestartStrategyBuilder extends FlinkRestartStrategyBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(FailureRateRestartStrategyBuilder.class);

  private static final int PARALLELISM_DIVIDE_NUMBER = 1000;

  private static final double RESTART_STRATEGY_ATTEMPTS_RATIO_DEFAULT = 0.02d;

  private int globalParallelism;

  @Override
  public RestartStrategies.RestartStrategyConfiguration buildRestartStrategyConf() {
    this.globalParallelism = parallelismAdvisor.getGlobalParallelism();

    String restartRatio = commonConf.get(CommonOptions.RestartOptions.RESTART_STRATEGY_ATTEMPTS_RATIO);
    int failureRate = calculateFailureRate(globalParallelism, restartRatio);
    int restartInterval = commonConf.get(CommonOptions.RestartOptions.RESTART_STRATEGY_RESTART_INTERVAL);
    int restartDelay = commonConf.get(CommonOptions.RestartOptions.RESTART_STRATEGY_RESTART_DELAY);

    LOG.info("Max failure rate: {}.", failureRate);
    return RestartStrategies.failureRateRestart(
        failureRate,
        Time.of(restartInterval, TimeUnit.SECONDS),
        Time.of(restartDelay, TimeUnit.SECONDS)
    );
  }

  private int calculateFailureRate(int globalParallelism,
                                   String restartRatio) {
    double failureRate = globalParallelism;
    if (StringUtils.isNotEmpty(restartRatio) && Double.parseDouble(restartRatio) > 0) {
      double ratio = Double.parseDouble(restartRatio);
      failureRate = Math.floor(globalParallelism * ratio);
      return (int) failureRate;
    }

    if (globalParallelism >= PARALLELISM_DIVIDE_NUMBER) {
      failureRate = Math.floor(globalParallelism * RESTART_STRATEGY_ATTEMPTS_RATIO_DEFAULT);
    }
    return (int) failureRate;
  }
}
