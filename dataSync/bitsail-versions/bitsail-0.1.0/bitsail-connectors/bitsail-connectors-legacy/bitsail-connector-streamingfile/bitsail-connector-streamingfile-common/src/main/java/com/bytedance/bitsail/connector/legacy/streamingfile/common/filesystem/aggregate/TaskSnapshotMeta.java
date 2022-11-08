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

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMeta;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Created 2020/12/10.
 */
public class TaskSnapshotMeta implements Serializable {
  private Integer taskId;
  private Long taskSnapshotTimestamp;
  private FileSystemMeta fileSystemMeta;
  private Long checkpointId;
  private boolean notifyCheckpointPhase;
  @Getter
  @Setter
  private Set<String> pendingCommitPartitions;

  public TaskSnapshotMeta(Integer taskId,
                          Long taskSnapshotTimestamp,
                          FileSystemMeta fileSystemMeta,
                          Long checkpointId,
                          boolean notifyCheckpointPhase) {
    this.taskId = taskId;
    this.taskSnapshotTimestamp = taskSnapshotTimestamp;
    this.fileSystemMeta = fileSystemMeta;
    this.checkpointId = checkpointId;
    this.notifyCheckpointPhase = notifyCheckpointPhase;
    this.pendingCommitPartitions = new HashSet<>();
  }

  public Integer getTaskId() {
    return taskId;
  }

  public Long getTaskSnapshotTimestamp() {
    return taskSnapshotTimestamp;
  }

  public FileSystemMeta getFileSystemMeta() {
    return fileSystemMeta;
  }

  public Long getCheckpointId() {
    return checkpointId;
  }

  public boolean isNotifyCheckpointPhase() {
    return notifyCheckpointPhase;
  }
}
