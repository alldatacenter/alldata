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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.filestate;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.directory.PartitionDirectoryManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.FileNameUtils;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.MapUtils;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class FileStateBasedOnTimestamp extends AbstractFileState {
  private String compressionExtension;
  private Map<String, List<Long>> currentPendingFileTimestamp;

  @Builder
  public FileStateBasedOnTimestamp(long checkpointId, int taskId, String compressionExtension, Map<String, List<Long>> currentPendingFileTimestamp) {
    super(checkpointId, taskId);
    this.compressionExtension = compressionExtension;
    this.currentPendingFileTimestamp = currentPendingFileTimestamp;
  }

  public FileStateBasedOnTimestamp(FileStateFactory.FileStateType fileStateType, DataInputView in) throws IOException {
    deserializeV1(fileStateType, in);
  }

  @Override
  FileStateFactory.FileStateType getFileStateType() {
    return FileStateFactory.FileStateType.TIMESTAMP;
  }

  @Override
  void setCurrentState(List<PartFileInfo> partFileInfos) {
    this.currentPendingFileTimestamp = new HashMap<>(partFileInfos.size());
    for (PartFileInfo partFileInfo : partFileInfos) {
      String partition = partFileInfo.getPartition();
      List<Long> timestamps = currentPendingFileTimestamp.getOrDefault(partition, new ArrayList<>());
      timestamps.add(partFileInfo.getCreationTime());
      currentPendingFileTimestamp.put(partition, timestamps);
    }
  }

  @Override
  public Map<String, List<String>> getPendingFileName(long jobId, PartitionDirectoryManager.DirectoryType directoryManager, int hourIndex) {
    if (MapUtils.isEmpty(currentPendingFileTimestamp)) {
      return new HashMap<>(1);
    }
    Map<String, List<String>> pendingFileName = new HashMap<>(currentPendingFileTimestamp.size());
    for (Map.Entry<String, List<Long>> fileTimestamp : currentPendingFileTimestamp.entrySet()) {
      pendingFileName.put(fileTimestamp.getKey(),
          fileTimestamp.getValue().stream().map(timestamp -> {
            String partition = fileTimestamp.getKey();
            String fileName = FileNameUtils.newFileName(FileNameUtils.SupportedVersion.version1.getVersionNum(),
                jobId, taskId, checkpointId, timestamp, compressionExtension);
            return directoryManager.getPartitionFileName(fileName, hourIndex, partition);
          }).collect(Collectors.toList()));
    }
    return pendingFileName;
  }

  @Override
  void serializeFileStateV1(DataOutputSerializer dataOutputView) throws IOException {
    dataOutputView.writeLong(this.checkpointId);
    dataOutputView.writeInt(this.taskId);
    if (null == compressionExtension) {
      dataOutputView.writeBoolean(true);
    } else {
      dataOutputView.writeBoolean(false);
      dataOutputView.writeUTF(compressionExtension);
    }
    Map<String, List<Long>> currentPendingTimestamp = this.currentPendingFileTimestamp;
    dataOutputView.writeInt(currentPendingTimestamp.size());
    for (Map.Entry<String, List<Long>> pendingTimestamp : currentPendingTimestamp.entrySet()) {
      dataOutputView.writeUTF(pendingTimestamp.getKey());
      dataOutputView.writeInt(pendingTimestamp.getValue().size());
      for (Long timestamp : pendingTimestamp.getValue()) {
        dataOutputView.writeLong(timestamp);
      }
    }
  }

  @Override
  AbstractFileState deserializeFileStateV1(DataInputView in) throws IOException {
    this.checkpointId = in.readLong();
    this.taskId = in.readInt();
    boolean compressionIsNull = in.readBoolean();
    this.compressionExtension = null;
    if (!compressionIsNull) {
      this.compressionExtension = in.readUTF();
    }
    int mapSize = in.readInt();
    Map<String, List<Long>> currentPendingTimestamp = new HashMap<>(mapSize);
    for (int i = 0; i < mapSize; i++) {
      String partition = in.readUTF();
      int listSize = in.readInt();
      List<Long> timestamps = new ArrayList<>(listSize);
      for (int j = 0; j < listSize; j++) {
        timestamps.add(in.readLong());
      }
      currentPendingTimestamp.put(partition, timestamps);
    }
    this.currentPendingFileTimestamp = currentPendingTimestamp;
    return this;
  }
}
