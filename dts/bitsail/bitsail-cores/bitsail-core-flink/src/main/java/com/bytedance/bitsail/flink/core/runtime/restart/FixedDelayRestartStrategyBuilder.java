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

import com.bytedance.bitsail.flink.core.option.FlinkCommonOptions;

import org.apache.flink.api.common.ExecutionMode;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FixedDelayRestartStrategyBuilder extends FlinkRestartStrategyBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(FixedDelayRestartStrategyBuilder.class);

  private String failoverStrategyName;
  private int delay;
  private int attempts;
  private double ratio;

  @Override
  public RestartStrategies.RestartStrategyConfiguration buildRestartStrategyConf() {
    this.failoverStrategyName = commonConf.get(FlinkCommonOptions.FLINK_TASK_FAILOVER_STRATEGY);
    this.delay = commonConf.get(FlinkCommonOptions.FLINK_RESTART_DELAY);
    this.attempts = commonConf.get(FlinkCommonOptions.FLINK_REGION_FAILOVER_ATTEMPTS_NUM);
    this.ratio = commonConf.get(FlinkCommonOptions.FLINK_RESTART_ATTEMPTS_RATIO);

    FailoverStrategy failoverStrategy = FailoverStrategy.valueOf(failoverStrategyName.toUpperCase());
    if (!FailoverStrategy.REGION.equals(failoverStrategy)) {
      return null;
    }

    int failureNumber = calculateFailureNumber(executionMode,
        parallelismAdvisor.getReaderParallelismList(),
        parallelismAdvisor.getWriterParallelismList());
    LOG.info("Region failover is configured, and fixed delay restart strategy will configure. " +
        "Restart attempts number is " + failureNumber);
    return RestartStrategies.fixedDelayRestart(failureNumber, Time.of(delay, TimeUnit.SECONDS));
  }

  private int calculateFailureNumber(ExecutionMode executionMode,
                                     List<Integer> readerParallelisms,
                                     List<Integer> writerParallelisms) {
    if (attempts > 0) {
      return attempts;
    }
    double ratio = this.ratio;
    if (executionMode == ExecutionMode.BATCH
        || executionMode == ExecutionMode.BATCH_FORCED) {
      ratio = 2 * ratio;
    }
    int maxReaderParallelism = Collections.max(readerParallelisms);
    int maxWriterParallelism = Collections.max(writerParallelisms);
    return (int) Math.ceil(ratio * Math.max(maxReaderParallelism, maxWriterParallelism));
  }

  enum FailoverStrategy {
    FULL,
    REGION,
    BATCHJOBFAILOVER;
  }
}
