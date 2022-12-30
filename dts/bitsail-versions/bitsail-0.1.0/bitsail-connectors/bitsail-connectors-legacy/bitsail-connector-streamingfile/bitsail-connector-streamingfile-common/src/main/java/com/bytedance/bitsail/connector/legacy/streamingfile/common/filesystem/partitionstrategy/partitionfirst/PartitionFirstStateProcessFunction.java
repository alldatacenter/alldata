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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.partitionfirst;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.StreamingJobCommitStatus;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.aggregate.GlobalAggregateResult;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.aggregate.TaskSnapshotMeta;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.AbstractPartitionStateProcessFunction;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeutils.base.StringSerializer;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class PartitionFirstStateProcessFunction extends AbstractPartitionStateProcessFunction {
  private static final int DEFAULT_CACHE_SIZE = 200;
  static final ListStateDescriptor<String> PENDING_COMMIT_PARTITIONS_STATE_DESC =
      new ListStateDescriptor<>("pending-commit-partitions", StringSerializer.INSTANCE);
  private final transient PartitionCache localAddedPartitions;
  private transient Set<String> pendingCommitPartitions;
  private transient ListState<String> pendingCommitPartitionsState;
  private transient Set<String> committedPartitions;

  public PartitionFirstStateProcessFunction() {
    localAddedPartitions = new PartitionCache(DEFAULT_CACHE_SIZE);
    committedPartitions = new HashSet<>();
  }

  @Override
  public void initializeState(FunctionInitializationContext context) throws Exception {
    this.pendingCommitPartitions = Sets.newHashSet();
    pendingCommitPartitionsState = context.getOperatorStateStore().getListState(PENDING_COMMIT_PARTITIONS_STATE_DESC);
    if (context.isRestored()) {
      this.pendingCommitPartitions = initializePendingCommitPartitions(pendingCommitPartitionsState);
      log.info("Pending commit partition size is " + this.pendingCommitPartitions.size());
      this.localAddedPartitions.addAll(this.pendingCommitPartitions);
    }
  }

  private Set<String> initializePendingCommitPartitions(final ListState<String> pendingCommitPartitionsState) throws Exception {
    Set<String> distinctPendingCommitPartitions = Sets.newHashSet();
    for (String pendingCommitPartitionTimeState : pendingCommitPartitionsState.get()) {
      distinctPendingCommitPartitions.add(pendingCommitPartitionTimeState);
    }
    return distinctPendingCommitPartitions;
  }

  @Override
  public void snapshotState(FunctionSnapshotContext context) throws Exception {
    pendingCommitPartitionsState.clear();
    pendingCommitPartitionsState.addAll(Lists.newArrayList(pendingCommitPartitions));
  }

  @Override
  public void updateStatusFromAggregateResult(GlobalAggregateResult aggregateResult) {
    this.committedPartitions = aggregateResult.getCommittedPartitions();
    pendingCommitPartitions.removeIf(committedPartitions::contains);
    if (pendingCommitPartitions.size() > 0) {
      log.info("Pending commit partition size is {} after update from aggregate result with committed partition size {}.",
          this.pendingCommitPartitions.size(), committedPartitions.size());
    }
  }

  @Override
  public void updateStreamingJobCommitStatus(StreamingJobCommitStatus streamingJobCommitStatus) {
    streamingJobCommitStatus.setPendingCommitPartitions(pendingCommitPartitions);
  }

  @Override
  public void updateStatusInSnapshot(List<PartFileInfo> partFileInfos) {
    for (PartFileInfo partFileInfo : partFileInfos) {
      String partition = partFileInfo.getPartition();
      if (!contains(partition)) {
        this.pendingCommitPartitions.add(partition);
        this.localAddedPartitions.add(partition);
      }
    }
  }

  @Override
  public void updateSnapshotMeta(TaskSnapshotMeta snapshotMeta) {
    snapshotMeta.setPendingCommitPartitions(this.pendingCommitPartitions);
  }

  private boolean contains(String partition) {
    return localAddedPartitions.contains(partition) || committedPartitions.contains(partition);
  }

}
