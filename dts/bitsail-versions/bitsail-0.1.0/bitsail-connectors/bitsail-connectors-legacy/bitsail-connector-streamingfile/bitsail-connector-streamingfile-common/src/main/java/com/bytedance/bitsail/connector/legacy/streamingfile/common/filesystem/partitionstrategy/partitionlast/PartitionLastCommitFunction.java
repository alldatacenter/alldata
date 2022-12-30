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

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.aggregate.StreamingJobAccumulator;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.AbstractPartitionCommitFunction;

import org.apache.flink.api.java.tuple.Tuple2;

import java.util.Set;
import java.util.stream.Collectors;

public class PartitionLastCommitFunction extends AbstractPartitionCommitFunction {
  public PartitionLastCommitFunction(BitSailConfiguration jobConf, int numberOfTasks) {
    super(numberOfTasks);
  }

  @Override
  public void commitPartitions(StreamingJobAccumulator jobAccumulator, AbstractPartitionCommitter partitionCommitter) throws Exception {
    if (jobAccumulator.getTaskCommittedCheckpoints().size() == numberOfTasks
        && jobAccumulator.getCurrentTaskSnapshotMeta().isNotifyCheckpointPhase()) {
      Set<Tuple2<Long, Long>> calculatePendingCommitPartitionTasks = jobAccumulator.calculatePendingCommitPartitionTasks();
      partitionCommitter.commitPartition(
          calculatePendingCommitPartitionTasks.stream().map(value -> value.f1)
              .collect(Collectors.toList()));
      jobAccumulator.removeCommittedPartitionTimes(calculatePendingCommitPartitionTasks);
    }
  }
}

