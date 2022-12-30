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

package com.bytedance.bitsail.connector.legacy.hive.runtime;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.operators.testutils.MockEnvironmentBuilder;
import org.apache.flink.runtime.state.OperatorStateBackend;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.streaming.runtime.tasks.TestProcessingTimeService;

import java.util.Collections;

public class MockStreamingRuntimeContextForTest extends StreamingRuntimeContext {
  private int attempNum;

  public MockStreamingRuntimeContextForTest() {
    super(new MockStreamOperator(), (new MockEnvironmentBuilder()).setTaskName("mockTask").build(), Collections.emptyMap());
  }

  public int getAttempNum() {
    return attempNum;
  }

  public void setAttempNum(int attempNum) {
    this.attempNum = attempNum;
  }

  private static class MockStreamOperator extends AbstractStreamOperator<Integer> {
    private static final long serialVersionUID = -1153976702711944427L;
    private transient TestProcessingTimeService testProcessingTimeService;

    private MockStreamOperator() {
    }

    public ExecutionConfig getExecutionConfig() {
      return new ExecutionConfig();
    }

    public OperatorID getOperatorID() {
      return new OperatorID();
    }

    public OperatorStateBackend getOperatorStateBackend() {
      return null;
    }
  }
}
