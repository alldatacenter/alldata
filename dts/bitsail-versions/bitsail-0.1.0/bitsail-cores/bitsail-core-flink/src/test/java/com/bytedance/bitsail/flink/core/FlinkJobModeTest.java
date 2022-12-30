/*
 * Copyright 2022 ByteDance and/or its affiliates
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.flink.core;

import com.bytedance.bitsail.base.runtime.progress.JobProgressPlugin;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.flink.core.runtime.restart.RestartStrategy;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableEnvironment;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class FlinkJobModeTest {

  @Test
  public void testStreamingMode() {
    FlinkJobMode streamingMode = FlinkJobMode.STREAMING;
    assertEquals(streamingMode.getRestartStrategy(), RestartStrategy.FAILURE_RATE_RESTART);

    List<Class> runtimePluginList = streamingMode.getRuntimePluginClasses();
    assertEquals(runtimePluginList.size(), 1);
    assertEquals(runtimePluginList.get(0), JobProgressPlugin.class);
    StreamExecutionEnvironment executionEnvironment = streamingMode.getStreamExecutionEnvironment(BitSailConfiguration.newDefault());
    TableEnvironment ignore = streamingMode.getStreamTableEnvironment(executionEnvironment);
  }

  @Test
  public void testBatchMode() {
    FlinkJobMode batchMode = FlinkJobMode.BATCH;
    assertEquals(batchMode.getRestartStrategy(), RestartStrategy.FIXED_DELAY_RESTART);

    List<Class> runtimePluginList = batchMode.getRuntimePluginClasses();
    assertEquals(runtimePluginList.size(), 2);
    assertEquals(runtimePluginList.get(0), JobProgressPlugin.class);
    StreamExecutionEnvironment executionEnvironment = batchMode.getStreamExecutionEnvironment(BitSailConfiguration.newDefault());
    TableEnvironment ignore = batchMode.getStreamTableEnvironment(executionEnvironment);
  }
}
