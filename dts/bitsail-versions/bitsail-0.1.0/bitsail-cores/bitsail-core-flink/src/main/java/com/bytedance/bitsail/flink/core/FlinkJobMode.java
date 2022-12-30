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

package com.bytedance.bitsail.flink.core;

import com.bytedance.bitsail.base.runtime.RuntimePlugin;
import com.bytedance.bitsail.base.runtime.metrics.BitSailClientMetricsPlugin;
import com.bytedance.bitsail.base.runtime.progress.JobProgressPlugin;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.flink.core.option.FlinkCommonOptions;
import com.bytedance.bitsail.flink.core.runtime.restart.RestartStrategy;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.ExecutionMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@Getter
public enum FlinkJobMode {

  STREAMING("BitSail_Flink_Streaming_Job",
      RestartStrategy.FAILURE_RATE_RESTART,
      JobProgressPlugin.class) {
    @Override
    public StreamTableEnvironment getStreamTableEnvironment(StreamExecutionEnvironment executionEnvironment) {
      return StreamTableEnvironment.create(
          executionEnvironment,
          EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build());
    }
  },

  BATCH("BitSail_Flink_Batch_Job",
      RestartStrategy.FIXED_DELAY_RESTART,
      JobProgressPlugin.class,
      BitSailClientMetricsPlugin.class) {
    @Override
    public StreamExecutionEnvironment getStreamExecutionEnvironment(BitSailConfiguration commonConfiguration) {
      StreamExecutionEnvironment executionEnvironment = super.getStreamExecutionEnvironment(commonConfiguration);

      try {
        Method batchModeMethod = StreamExecutionEnvironment.class.getMethod("useBatchMode");
        batchModeMethod.setAccessible(true);
        batchModeMethod.invoke(executionEnvironment);
      } catch (Exception e) {
        //ignore
      }
      ExecutionMode executionMode = ExecutionMode.valueOf(StringUtils
          .upperCase(commonConfiguration.get(FlinkCommonOptions.EXECUTION_MODE)));

      executionEnvironment.getConfig().setExecutionMode(executionMode);
      executionEnvironment.getConfig().enableObjectReuse();

      return executionEnvironment;
    }

    @Override
    public TableEnvironment getStreamTableEnvironment(StreamExecutionEnvironment executionEnvironment) {
      return TableEnvironment.create(
          EnvironmentSettings.newInstance().inBatchMode()
              .useBlinkPlanner().build());
    }
  };

  private final String jobName;

  private final RestartStrategy restartStrategy;

  /**
   * should be subclass of {@link RuntimePlugin}
   */
  private final List<Class> runtimePluginClasses;

  FlinkJobMode(String jobName, RestartStrategy restartStrategy, Class... runtimePluginClasses) {
    this.jobName = jobName;
    this.restartStrategy = restartStrategy;
    this.runtimePluginClasses = Arrays.asList(runtimePluginClasses);

    Arrays.stream(runtimePluginClasses).forEach(runtimePluginClass ->
        Preconditions.checkState(RuntimePlugin.class.isAssignableFrom(runtimePluginClass),
            runtimePluginClass.getName() + " is not subclass of com.bytedance.bitsail.base.runtime.plugin.RuntimePlugin"));
  }

  public StreamExecutionEnvironment getStreamExecutionEnvironment(BitSailConfiguration commonConfiguration) {
    return StreamExecutionEnvironment.getExecutionEnvironment();
  }

  public TableEnvironment getStreamTableEnvironment(StreamExecutionEnvironment executionEnvironment) {
    throw new UnsupportedOperationException();
  }
}
