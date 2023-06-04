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

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.aggregate.StreamingJobAccumulator;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.AbstractPartitionCommitFunction;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PartitionFirstCommitFunction extends AbstractPartitionCommitFunction {
  private final int commitPartitionSize;
  private final long commitPartitionTime;

  public PartitionFirstCommitFunction(BitSailConfiguration jobConf, int numberOfTasks) {
    super(numberOfTasks);
    commitPartitionSize = jobConf.get(FileSystemCommonOptions.CommitOptions.COMMIT_PARTITION_SIZE);
    commitPartitionTime = TimeUnit.SECONDS.toMillis(jobConf.get(FileSystemCommonOptions.CommitOptions.COMMIT_PARTITION_TIME));
  }

  @Override
  public void commitPartitions(StreamingJobAccumulator jobAccumulator, AbstractPartitionCommitter partitionCommitter) {
    if (!(jobAccumulator.getTaskCommittedCheckpoints().size() == numberOfTasks
        && jobAccumulator.getCurrentTaskSnapshotMeta().isNotifyCheckpointPhase())) {
      return;
    }
    Set<String> pendingCommitPartitions = jobAccumulator.getJobCommitStatus().getPendingCommitPartitions();
    long currentCheckpointId = jobAccumulator
        .getTaskCommittedCheckpoints().values().stream().mapToLong(value -> value).max().getAsLong();
    Set<String> currentCommittedPartitions = new HashSet<>(pendingCommitPartitions.size());
    Set<String> committedPartitions = jobAccumulator.getCommittedPartitions();
    int committedSize = 0;
    long startTime = System.currentTimeMillis();
    for (String partition : pendingCommitPartitions) {
      if (!committedPartitions.contains(partition)) {
        try {
          partitionCommitter.commitPartition(partition);
          committedSize++;
        } catch (Exception e) {
          throw new RuntimeException("Aggregate Commit failed.", e);
        }
      }
      currentCommittedPartitions.add(partition);
      long currentTime = System.currentTimeMillis();
      long duration = currentTime - startTime;
      if (committedSize >= commitPartitionSize || duration > commitPartitionTime) {
        break;
      }
    }
    pendingCommitPartitions.removeAll(currentCommittedPartitions);
    jobAccumulator.updateCommittedPartitions(currentCheckpointId, currentCommittedPartitions);
    jobAccumulator.getJobCommitStatus().setPendingCommitPartitions(pendingCommitPartitions);

    updateAccumulatorCommittedPartitions(jobAccumulator);
  }

  private void updateAccumulatorCommittedPartitions(StreamingJobAccumulator jobAccumulator) {
    if (jobAccumulator.getTaskCommittedCheckpoints().size() == numberOfTasks
        && jobAccumulator.getCurrentTaskSnapshotMeta().isNotifyCheckpointPhase()) {
      long minCheckpointId = jobAccumulator.getTaskCommittedCheckpoints().values().stream().mapToLong(value -> value).min().getAsLong();
      Map<Long, Set<String>> checkpointToCommittedPartitionsMap = jobAccumulator.getCheckpointToCommittedPartitionsMap();
      cleanCommittedPartitions(minCheckpointId, checkpointToCommittedPartitionsMap);
      jobAccumulator.setCheckpointToCommittedPartitionsMap(checkpointToCommittedPartitionsMap);
    }
  }

  void cleanCommittedPartitions(long checkpointId, Map<Long, Set<String>> checkpointToCommittedPartitionsMap) {
    Iterator<Map.Entry<Long, Set<String>>> it = checkpointToCommittedPartitionsMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Long, Set<String>> entry = it.next();
      long cpId = entry.getKey();
      if (cpId < checkpointId) {
        it.remove();
      }
    }
  }
}
