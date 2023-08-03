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

import org.apache.flink.runtime.checkpoint.OperatorSubtaskState;
import org.apache.flink.runtime.checkpoint.PrioritizedOperatorSubtaskState;
import org.apache.flink.runtime.checkpoint.TaskStateSnapshot;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.operators.testutils.MockInputSplitProvider;
import org.apache.flink.runtime.state.TestTaskStateManager;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class TestOneInputStreamOperatorIntern<IN, OUT> extends OneInputStreamOperatorTestHarness<IN, OUT> {
  public TestOneInputStreamOperatorIntern(
      OneInputStreamOperator<IN, OUT> operator,
      int maxParallelism,
      int parallelism,
      int subtaskIndex,
      Long restoredCheckpointId,
      TestGlobalAggregateManager testGlobalAggregateManager) throws Exception {
    super(
        operator,
        (new MockEnvironmentBuilder())
            .setTaskName("MockTask")
            .setManagedMemorySize(3145728L)
            .setInputSplitProvider(new MockInputSplitProvider())
            .setBufferSize(1024)
            .setTaskStateManager(new TestTaskStateManagerIntern(restoredCheckpointId))
            .setAggregateManager(testGlobalAggregateManager)
            .setMaxParallelism(maxParallelism)
            .setParallelism(parallelism)
            .setSubtaskIndex(subtaskIndex)
            .build()
    );
  }

  public void notifyOfAbortedCheckpoint(long checkpointId) throws Exception {
    this.operator.notifyCheckpointAborted(checkpointId);
  }

  static class TestTaskStateManagerIntern extends TestTaskStateManager {
    private long reportedCheckpointId = -1L;
    private boolean restored = false;

    public TestTaskStateManagerIntern(Long reportedCheckpointId) {
      super();
      if (reportedCheckpointId != null) {
        this.reportedCheckpointId = reportedCheckpointId;
        this.restored = true;
      }
    }

    @Nonnull
    public PrioritizedOperatorSubtaskState prioritizedOperatorState(OperatorID operatorID) {
      TaskStateSnapshot jmTaskStateSnapshot = this.getLastJobManagerTaskStateSnapshot();
      TaskStateSnapshot tmTaskStateSnapshot = this.getLastTaskManagerTaskStateSnapshot();
      if (jmTaskStateSnapshot == null) {
        return PrioritizedOperatorSubtaskState.emptyNotRestored();
      } else {
        OperatorSubtaskState jmOpState = jmTaskStateSnapshot.getSubtaskStateByOperatorID(operatorID);
        if (jmOpState == null) {
          return PrioritizedOperatorSubtaskState.emptyNotRestored();
        } else {
          List<OperatorSubtaskState> tmStateCollection = Collections.emptyList();
          if (tmTaskStateSnapshot != null) {
            OperatorSubtaskState tmOpState = tmTaskStateSnapshot.getSubtaskStateByOperatorID(operatorID);
            if (tmOpState != null) {
              tmStateCollection = Collections.singletonList(tmOpState);
            }
          }

          PrioritizedOperatorSubtaskState.Builder builder =
              new PrioritizedOperatorSubtaskState.Builder(jmOpState, tmStateCollection, this.restored);
          return builder.build();
        }
      }
    }
  }

}
