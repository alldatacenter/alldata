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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.partitionlast;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.StreamingJobCommitStatus;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.aggregate.GlobalAggregateResult;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.AbstractPartitionStateProcessFunction;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;

import java.util.Set;

public class PartitionLastStateProcessFunction extends AbstractPartitionStateProcessFunction {
  static final ListStateDescriptor<Tuple2<Long, Long>> PENDING_COMMIT_PARTITION_TIMES_STATE_DESC =
      new ListStateDescriptor<>("pending-commit-partition-times", TypeInformation.of(new TypeHint<Tuple2<Long, Long>>() {
      }));
  private transient Set<Tuple2<Long, Long>> pendingCommitPartitionTaskTimes;
  private transient ListState<Tuple2<Long, Long>> pendingCommitPartitionTimesState;

  @Override
  public void initializeState(FunctionInitializationContext context) throws Exception {
    this.pendingCommitPartitionTaskTimes = Sets.newHashSet();
    pendingCommitPartitionTimesState = context.getOperatorStateStore().getUnionListState(PENDING_COMMIT_PARTITION_TIMES_STATE_DESC);
    if (context.isRestored()) {
      initializePendingCommitPartitionTime(pendingCommitPartitionTimesState);
    }
  }

  @Override
  public void snapshotState(FunctionSnapshotContext context) throws Exception {
    pendingCommitPartitionTimesState.clear();
    pendingCommitPartitionTimesState.addAll(Lists.newArrayList(pendingCommitPartitionTaskTimes));
  }

  private void initializePendingCommitPartitionTime(final ListState<Tuple2<Long, Long>> pendingCommitPartitionTimesState) throws Exception {
    Set<Tuple2<Long, Long>> distinctPendingCommitPartitionTimes = Sets.newHashSet();
    for (Tuple2<Long, Long> pendingCommitPartitionTimeState : pendingCommitPartitionTimesState.get()) {
      distinctPendingCommitPartitionTimes.add(pendingCommitPartitionTimeState);
    }
    this.pendingCommitPartitionTaskTimes = distinctPendingCommitPartitionTimes;
  }

  @Override
  public void updateStatusFromAggregateResult(GlobalAggregateResult aggregateResult) {
    this.pendingCommitPartitionTaskTimes = aggregateResult.getPendingCommitPartitionTaskTimes();
  }

  @Override
  public void updateStreamingJobCommitStatus(StreamingJobCommitStatus streamingJobCommitStatus) {
    streamingJobCommitStatus.setPendingCommitPartitionTaskTimes(pendingCommitPartitionTaskTimes);
  }
}
