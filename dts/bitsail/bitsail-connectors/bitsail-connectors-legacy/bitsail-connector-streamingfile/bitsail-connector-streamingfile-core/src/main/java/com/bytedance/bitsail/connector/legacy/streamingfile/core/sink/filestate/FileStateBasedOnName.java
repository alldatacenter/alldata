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

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@Slf4j
public class FileStateBasedOnName extends AbstractFileState {
  private Map<String, List<String>> currentPendingFileNames;

  @Builder
  public FileStateBasedOnName(long checkpointId, int taskId, Map<String, List<String>> currentPendingFileNames) {
    super(checkpointId, taskId);
    this.currentPendingFileNames = currentPendingFileNames;
  }

  public FileStateBasedOnName(FileStateFactory.FileStateType fileStateType, DataInputView in) throws IOException {
    super();
    deserializeV1(fileStateType, in);
  }

  @Override
  FileStateFactory.FileStateType getFileStateType() {
    return FileStateFactory.FileStateType.FILE_NAME;
  }

  @Override
  void setCurrentState(List<PartFileInfo> partFileInfos) {
    this.currentPendingFileNames = new HashMap<>(partFileInfos.size());
    for (PartFileInfo partFileInfo : partFileInfos) {
      String partition = partFileInfo.getPartition();
      List<String> fileNames = currentPendingFileNames.getOrDefault(partition, new ArrayList<>());
      fileNames.add(partFileInfo.getPath().getName());
      currentPendingFileNames.put(partition, fileNames);
    }
  }

  @Override
  public Map<String, List<String>> getPendingFileName(long jobId, PartitionDirectoryManager.DirectoryType directoryManager, int hourIndex) {
    if (MapUtils.isNotEmpty(currentPendingFileNames)) {
      return currentPendingFileNames;
    }
    return new HashMap<>(1);
  }

  @Override
  void serializeFileStateV1(DataOutputSerializer dataOutputView) throws IOException {
    dataOutputView.writeLong(checkpointId);
    dataOutputView.writeInt(taskId);
    Map<String, List<String>> currentPendingFileName = this.currentPendingFileNames;
    dataOutputView.writeInt(currentPendingFileName.size());
    for (Map.Entry<String, List<String>> pendingFileName : currentPendingFileName.entrySet()) {
      dataOutputView.writeUTF(pendingFileName.getKey());
      dataOutputView.writeInt(pendingFileName.getValue().size());
      for (String fileName : pendingFileName.getValue()) {
        dataOutputView.writeUTF(fileName);
      }
    }
  }

  @Override
  AbstractFileState deserializeFileStateV1(DataInputView in) throws IOException {
    this.checkpointId = in.readLong();
    this.taskId = in.readInt();
    int mapSize = in.readInt();
    Map<String, List<String>> currentPendingFileName = new HashMap<>(mapSize);
    for (int i = 0; i < mapSize; i++) {
      String partition = in.readUTF();
      int listSize = in.readInt();
      List<String> fileNames = new ArrayList<>(listSize);
      for (int j = 0; j < listSize; j++) {
        fileNames.add(in.readUTF());
      }
      currentPendingFileName.put(partition, fileNames);
    }
    this.currentPendingFileNames = currentPendingFileName;
    return this;
  }
}
