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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;

import org.apache.flink.api.java.tuple.Tuple2;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @class: StreamingJobCommitStatus
 * @desc:
 **/
public class StreamingJobCommitStatus implements Serializable {
  private Long nextCommitPartitionTime;
  private Set<Tuple2<Long, Long>> pendingCommitPartitionTaskTimes;
  private Set<String> pendingCommitPartitions;

  public StreamingJobCommitStatus(final BitSailConfiguration jobConf) {
    String formatType = jobConf.get(FileSystemCommonOptions.DUMP_FORMAT_TYPE);
    switch (formatType) {
      case StreamingFileSystemValidator.HDFS_FORMAT_TYPE_VALUE:
      case StreamingFileSystemValidator.HIVE_FORMAT_TYPE_VALUE:
        break;
      default:
        throw new RuntimeException("Unsupported format type: " + formatType);
    }
    nextCommitPartitionTime = Long.MIN_VALUE;
    pendingCommitPartitionTaskTimes = new HashSet<>();
    pendingCommitPartitions = new HashSet<>();

  }

  public Long getJobCommitTime() {
    return nextCommitPartitionTime;
  }

  public void setJobCommitTime(Long jobCommitTime) {
    this.nextCommitPartitionTime = jobCommitTime;
  }

  public Set<Tuple2<Long, Long>> getPendingCommitPartitionTaskTimes() {
    return pendingCommitPartitionTaskTimes;
  }

  public void setPendingCommitPartitionTaskTimes(Set<Tuple2<Long, Long>> pendingCommitPartitionTaskTimes) {
    this.pendingCommitPartitionTaskTimes = pendingCommitPartitionTaskTimes;
  }

  public Set<String> getPendingCommitPartitions() {
    return pendingCommitPartitions;
  }

  public void setPendingCommitPartitions(Set<String> pendingCommitPartitions) {
    this.pendingCommitPartitions = pendingCommitPartitions;
  }

  public void mergeJobCommitStatus(StreamingJobCommitStatus jobCommitStatus) {
    if (nextCommitPartitionTime < jobCommitStatus.getJobCommitTime()) {
      nextCommitPartitionTime = jobCommitStatus.getJobCommitTime();
    }
    pendingCommitPartitions.addAll(jobCommitStatus.getPendingCommitPartitions());
  }
}
