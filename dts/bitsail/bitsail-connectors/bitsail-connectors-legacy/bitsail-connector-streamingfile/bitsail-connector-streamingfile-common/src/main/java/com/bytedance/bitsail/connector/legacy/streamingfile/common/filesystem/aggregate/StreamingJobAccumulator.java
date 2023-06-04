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

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.StreamingJobCommitStatus;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMeta;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.java.tuple.Tuple2;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @class: StreamingJobAccumulator
 * @desc:
 **/
@Getter
@Setter
public class StreamingJobAccumulator implements Serializable {
  private Long lastMinTaskSnapshotTime;
  private Map<Integer, Long> taskSnapshotTimes;
  private StreamingJobCommitStatus jobCommitStatus;
  private FileSystemMeta fileSystemMeta;
  private Map<Integer, Long> taskCommittedCheckpoints;
  private transient TaskSnapshotMeta currentTaskSnapshotMeta;
  private Map<Long, Set<String>> checkpointToCommittedPartitionsMap;

  public StreamingJobAccumulator(StreamingJobCommitStatus jobCommitStatus) {
    this.lastMinTaskSnapshotTime = Long.MIN_VALUE;
    this.taskSnapshotTimes = new HashMap<>();
    this.jobCommitStatus = jobCommitStatus;
    this.taskCommittedCheckpoints = Maps.newHashMap();
    this.checkpointToCommittedPartitionsMap = Maps.newHashMap();
  }

  public void updateTaskSnapshotTime(TaskSnapshotMeta taskSnapshotTime) {
    taskSnapshotTimes.put(taskSnapshotTime.getTaskId(), taskSnapshotTime.getTaskSnapshotTimestamp());
  }

  public Set<String> getCommittedPartitions() {
    Set<String> result = new HashSet<>();
    for (Set<String> committedPartitions : checkpointToCommittedPartitionsMap.values()) {
      result.addAll(committedPartitions);
    }
    return result;
  }

  public void updateCommittedPartitions(long checkpointId, Set<String> committedPartitions) {
    Set<String> partitions = this.checkpointToCommittedPartitionsMap.getOrDefault(checkpointId, new HashSet<>());
    partitions.addAll(committedPartitions);
    this.checkpointToCommittedPartitionsMap.put(checkpointId, partitions);
  }

  public void updatePendingCommitPartitions(Set<String> pendingCommitPartitions) {
    this.jobCommitStatus.getPendingCommitPartitions().addAll(pendingCommitPartitions);
  }

  public Tuple2<Integer, Long> getMinTaskSnapshotTime() {
    int taskId = -1;
    long minSnapshotTime = Long.MAX_VALUE;

    for (Map.Entry<Integer, Long> entry : taskSnapshotTimes.entrySet()) {
      if (entry.getValue() < minSnapshotTime) {
        taskId = entry.getKey();
        minSnapshotTime = entry.getValue();
      }
    }
    if (minSnapshotTime == Long.MAX_VALUE) {
      minSnapshotTime = Long.MIN_VALUE;
    }
    return Tuple2.of(taskId, minSnapshotTime);
  }

  public void mergeJobAccumulator(StreamingJobAccumulator jobAccumulator) {
    taskSnapshotTimes.putAll(jobAccumulator.getTaskSnapshotTimes());
    taskCommittedCheckpoints.putAll(jobAccumulator.getTaskCommittedCheckpoints());
    jobCommitStatus.mergeJobCommitStatus(jobAccumulator.getJobCommitStatus());
    jobCommitStatus.getPendingCommitPartitionTaskTimes().addAll(jobAccumulator
        .getJobCommitStatus()
        .getPendingCommitPartitionTaskTimes());
    updateFileSystemMeta(jobAccumulator.getFileSystemMeta());
    updatePartitionInfo(jobAccumulator);
  }

  private void updatePartitionInfo(StreamingJobAccumulator jobAccumulator) {
    for (Map.Entry<Long, Set<String>> partitionsEntry : jobAccumulator.getCheckpointToCommittedPartitionsMap().entrySet()) {
      Set<String> partitions = checkpointToCommittedPartitionsMap.getOrDefault(partitionsEntry.getKey(), new HashSet<>());
      partitions.addAll(partitionsEntry.getValue());
      checkpointToCommittedPartitionsMap.put(partitionsEntry.getKey(), partitions);
    }
  }

  public void updateSingleTaskCheckpoint(Integer taskId, long taskCheckpointId) {
    taskCommittedCheckpoints.put(taskId, taskCheckpointId);
  }

  public void updateCheckpointPendingCommitPartitionTasks(long checkpointId, List<Long> pendingAppends) {
    if (CollectionUtils.isNotEmpty(pendingAppends)) {
      for (Long pendingCommitPartitionTaskTime : pendingAppends) {
        jobCommitStatus.getPendingCommitPartitionTaskTimes().add(new Tuple2<>(checkpointId, pendingCommitPartitionTaskTime));
      }
    }
  }

  public void removeCommittedPartitionTimes(Set<Tuple2<Long, Long>> committedPartitionTaskTimes) {
    if (committedPartitionTaskTimes.size() != 0) {
      for (Tuple2<Long, Long> committedPartitionTaskTime : committedPartitionTaskTimes) {
        jobCommitStatus.getPendingCommitPartitionTaskTimes().remove(committedPartitionTaskTime);
      }
    }
  }

  public Set<Tuple2<Long, Long>> calculatePendingCommitPartitionTasks() {
    long minCheckpointId = taskCommittedCheckpoints.values()
        .stream()
        .mapToLong(value -> value)
        .min()
        .orElse(Long.MIN_VALUE);

    Set<Tuple2<Long, Long>> triggerPendingCommitPartitionTimes = Sets.newHashSet();
    for (Tuple2<Long, Long> pendingCommitPartitionTaskTime : jobCommitStatus.getPendingCommitPartitionTaskTimes()) {
      if (pendingCommitPartitionTaskTime.f0 < minCheckpointId) {
        triggerPendingCommitPartitionTimes.add(pendingCommitPartitionTaskTime);
      }
    }
    return triggerPendingCommitPartitionTimes;
  }

  public void updateFileSystemMeta(TaskSnapshotMeta taskSnapshotTime) {
    updateFileSystemMeta(taskSnapshotTime.getFileSystemMeta());
  }

  private void updateFileSystemMeta(FileSystemMeta otherSchemaMeta) {
    if (Objects.isNull(fileSystemMeta)) {
      fileSystemMeta = otherSchemaMeta;
      return;
    }
    if (Objects.isNull(otherSchemaMeta)) {
      return;
    }
    if (fileSystemMeta.getCreateTime() < otherSchemaMeta.getCreateTime()) {
      fileSystemMeta = otherSchemaMeta;
    }
  }
}
