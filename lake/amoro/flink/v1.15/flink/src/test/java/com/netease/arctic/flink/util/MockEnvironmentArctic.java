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

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.JobID;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.externalresource.ExternalResourceInfoProvider;
import org.apache.flink.runtime.io.disk.iomanager.IOManager;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.memory.MemoryManager;
import org.apache.flink.runtime.metrics.groups.TaskMetricGroup;
import org.apache.flink.runtime.operators.testutils.MockEnvironment;
import org.apache.flink.runtime.operators.testutils.MockInputSplitProvider;
import org.apache.flink.runtime.state.TaskStateManager;
import org.apache.flink.runtime.taskexecutor.GlobalAggregateManager;
import org.apache.flink.runtime.taskmanager.TaskManagerRuntimeInfo;
import org.apache.flink.util.UserCodeClassLoader;

public class MockEnvironmentArctic extends MockEnvironment {

  protected MockEnvironmentArctic(
      JobID jobID, JobVertexID jobVertexID, String taskName, MockInputSplitProvider inputSplitProvider, int bufferSize,
      Configuration taskConfiguration, ExecutionConfig executionConfig, IOManager ioManager,
      TaskStateManager taskStateManager, GlobalAggregateManager aggregateManager, int maxParallelism, int parallelism,
      int subtaskIndex, UserCodeClassLoader userCodeClassLoader, TaskMetricGroup taskMetricGroup,
      TaskManagerRuntimeInfo taskManagerRuntimeInfo, MemoryManager memManager,
      ExternalResourceInfoProvider externalResourceInfoProvider) {
    super(
        jobID,
        jobVertexID,
        taskName,
        inputSplitProvider,
        bufferSize,
        taskConfiguration,
        executionConfig,
        ioManager,
        taskStateManager,
        aggregateManager,
        maxParallelism,
        parallelism,
        subtaskIndex,
        userCodeClassLoader,
        taskMetricGroup,
        taskManagerRuntimeInfo,
        memManager,
        externalResourceInfoProvider);
  }
}
