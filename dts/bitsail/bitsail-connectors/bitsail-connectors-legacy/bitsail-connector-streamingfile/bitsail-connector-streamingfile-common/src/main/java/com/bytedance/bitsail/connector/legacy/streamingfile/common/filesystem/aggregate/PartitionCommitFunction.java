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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.aggregate;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.StreamingJobCommitStatus;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.AbstractPartitionCommitFunction;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.PartitionStrategyFactory;

import lombok.SneakyThrows;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * calculate the minimum processing/event timestamp from current checkpoint id,
 * and add partition based on the timestamp.
 */
public class PartitionCommitFunction
    implements AggregateFunction<TaskSnapshotMeta, StreamingJobAccumulator, GlobalAggregateResult> {
  private static final Logger LOG = LoggerFactory.getLogger(PartitionCommitFunction.class);

  private final int numberOfTasks;
  private final AbstractPartitionCommitter partitionCommitter;
  private final AbstractPartitionCommitFunction abstractPartitionCommitFunction;
  private StreamingJobCommitStatus initJobCommitStatus;

  public PartitionCommitFunction(int numberOfTasks,
                                 AbstractPartitionCommitter partitionCommitter,
                                 StreamingJobCommitStatus initJobCommitStatus,
                                 BitSailConfiguration jobConf) {
    this.numberOfTasks = numberOfTasks;
    this.partitionCommitter = partitionCommitter;
    this.initJobCommitStatus = initJobCommitStatus;
    this.abstractPartitionCommitFunction = PartitionStrategyFactory
        .getPartitionStrategyCommitFunction(jobConf, numberOfTasks);
  }

  @Override
  public StreamingJobAccumulator createAccumulator() {
    return new StreamingJobAccumulator(initJobCommitStatus);
  }

  @Override
  public StreamingJobAccumulator add(TaskSnapshotMeta taskSnapshotMeta,
                                     StreamingJobAccumulator jobAccumulator) {
    if (taskSnapshotMeta.getTaskId() >= 0) {

      if (taskSnapshotMeta.getTaskSnapshotTimestamp() > 0) {
        jobAccumulator.updateTaskSnapshotTime(taskSnapshotMeta);
      }

      if (taskSnapshotMeta.getCheckpointId() >= 0) {
        jobAccumulator.updateSingleTaskCheckpoint(taskSnapshotMeta.getTaskId(),
            taskSnapshotMeta.getCheckpointId());
      }
    }

    jobAccumulator.setCurrentTaskSnapshotMeta(taskSnapshotMeta);
    jobAccumulator.updateFileSystemMeta(taskSnapshotMeta);
    jobAccumulator.updatePendingCommitPartitions(taskSnapshotMeta.getPendingCommitPartitions());
    return jobAccumulator;
  }

  @SneakyThrows
  @Override
  public GlobalAggregateResult getResult(StreamingJobAccumulator jobAccumulator) {
    GlobalAggregateResult.GlobalAggregateResultBuilder builder = GlobalAggregateResult.builder();
    builder.fileSystemMeta(jobAccumulator.getFileSystemMeta())
        .minJobTimestamp(calculateMinJobTimestamp(jobAccumulator))
        .pendingCommitPartitionTaskTimes(jobAccumulator.getJobCommitStatus().getPendingCommitPartitionTaskTimes())
        .committedPartitions(jobAccumulator.getCommittedPartitions());

    return builder.build();
  }

  private long calculateMinJobTimestamp(StreamingJobAccumulator jobAccumulator) throws Exception {
    long jobCommitTimeBefore = jobAccumulator.getJobCommitStatus().getJobCommitTime();
    long jobCommitTimeAfter = jobCommitTimeBefore;

    long lastMinTaskSnapshotTime = jobAccumulator.getLastMinTaskSnapshotTime();
    Tuple2<Integer, Long> minTaskSnapshot = jobAccumulator.getMinTaskSnapshotTime();
    int taskId = minTaskSnapshot.f0;
    long currentMinTaskSnapshotTime = minTaskSnapshot.f1;

    abstractPartitionCommitFunction.commitPartitions(jobAccumulator, partitionCommitter);

    if (jobAccumulator.getTaskSnapshotTimes().size() != numberOfTasks ||
        lastMinTaskSnapshotTime == currentMinTaskSnapshotTime) {
      return jobCommitTimeAfter;
    }

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try {
      List<Long> newPendingCommitPartitionTimes = partitionCommitter
          .commitAndUpdateJobTime(jobAccumulator.getJobCommitStatus(), currentMinTaskSnapshotTime);
      jobCommitTimeAfter = jobAccumulator.getJobCommitStatus().getJobCommitTime();
      jobAccumulator.setLastMinTaskSnapshotTime(currentMinTaskSnapshotTime);

      long currentCheckpointId = jobAccumulator
          .getTaskCommittedCheckpoints().values().stream().mapToLong(value -> value).max().getAsLong();
      jobAccumulator.updateCheckpointPendingCommitPartitionTasks(currentCheckpointId, newPendingCommitPartitionTimes);

      String minJobTimeStr = simpleDateFormat.format(currentMinTaskSnapshotTime);
      String commitBeforeTimeStr = simpleDateFormat.format(jobCommitTimeBefore);
      String commitAfterTimeStr = simpleDateFormat.format(jobCommitTimeAfter);
      LOG.info("commit partition, min task = {}, min job time={}, before commit time={}, after commit time={}",
          taskId, minJobTimeStr, commitBeforeTimeStr, commitAfterTimeStr);
      LOG.info("new pending commit partitions = {}.", newPendingCommitPartitionTimes);
      LOG.info("all pending commit partitions = {}.", jobAccumulator.getJobCommitStatus().getPendingCommitPartitionTaskTimes());
    } catch (Exception e) {
      throw new RuntimeException("Aggregate Commit failed.", e);
    }
    return jobCommitTimeAfter;
  }

  @Override
  public StreamingJobAccumulator merge(StreamingJobAccumulator a, StreamingJobAccumulator b) {
    StreamingJobAccumulator jobAccumulator = new StreamingJobAccumulator(initJobCommitStatus);
    jobAccumulator.mergeJobAccumulator(a);
    jobAccumulator.mergeJobAccumulator(b);
    return jobAccumulator;
  }
}
