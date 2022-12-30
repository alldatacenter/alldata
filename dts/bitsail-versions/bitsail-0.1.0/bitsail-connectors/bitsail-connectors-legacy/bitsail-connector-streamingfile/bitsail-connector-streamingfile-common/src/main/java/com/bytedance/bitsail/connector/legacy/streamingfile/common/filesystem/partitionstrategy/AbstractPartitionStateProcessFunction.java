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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.StreamingJobCommitStatus;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.aggregate.GlobalAggregateResult;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.aggregate.TaskSnapshotMeta;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;

import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;

import java.util.List;

public abstract class AbstractPartitionStateProcessFunction {
  /**
   * Initial partition strategy state from checkpoint state
   *
   * @param context flink context
   * @throws Exception exception
   */
  public abstract void initializeState(FunctionInitializationContext context) throws Exception;

  /**
   * snapshot current state
   *
   * @param context flink context
   * @throws Exception exception
   */
  public abstract void snapshotState(FunctionSnapshotContext context) throws Exception;

  /**
   * update status based on global aggregate result
   *
   * @param aggregateResult aggregate result from jm
   */
  public abstract void updateStatusFromAggregateResult(GlobalAggregateResult aggregateResult);

  /**
   * update status in snapshot, this is only use in partition first strategy right now
   *
   * @param partFileInfos created files in this checkpoint
   */
  public void updateStatusInSnapshot(List<PartFileInfo> partFileInfos) {
  }

  /**
   * update task snapshot meta based on partition strategy state
   *
   * @param snapshotMeta snapshot meta
   */
  public void updateSnapshotMeta(TaskSnapshotMeta snapshotMeta) {
  }

  /**
   * update streaming job commit status base on partition strategy state
   *
   * @param streamingJobCommitStatus streaming job commit status
   */
  public abstract void updateStreamingJobCommitStatus(StreamingJobCommitStatus streamingJobCommitStatus);

}
