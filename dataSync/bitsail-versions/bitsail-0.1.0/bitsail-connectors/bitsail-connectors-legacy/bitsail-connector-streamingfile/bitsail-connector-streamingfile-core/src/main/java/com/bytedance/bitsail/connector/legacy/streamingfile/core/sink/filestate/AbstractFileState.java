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

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionPathUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.directory.PartitionDirectoryManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.FileSystemCommitter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.core.fs.Path;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionTempFileManager.getTaskTemporaryPath;

@Slf4j
@NoArgsConstructor
@Getter
public abstract class AbstractFileState {
  protected long checkpointId;
  protected int taskId;

  public AbstractFileState(long checkpointId, int taskId) {
    this.checkpointId = checkpointId;
    this.taskId = taskId;
  }

  abstract FileStateFactory.FileStateType getFileStateType();

  abstract void setCurrentState(List<PartFileInfo> partFileInfos);

  public abstract Map<String, List<String>> getPendingFileName(long jobId, PartitionDirectoryManager.DirectoryType directoryManager, int hourIndex);

  public void serializeV1(DataOutputSerializer dataOutputView) throws IOException {
    serializeFileStateTypeV1(dataOutputView);
    serializeFileStateV1(dataOutputView);
  }

  abstract void serializeFileStateV1(DataOutputSerializer dataOutputView) throws IOException;

  private void serializeFileStateTypeV1(DataOutputSerializer dataOutputSerializer) throws IOException {
    dataOutputSerializer.writeByte(getFileStateType().getIndex());
  }

  private boolean validateFileStateType(FileStateFactory.FileStateType fileStateType) {
    return fileStateType.getIndex() == getFileStateType().getIndex();
  }

  abstract AbstractFileState deserializeFileStateV1(DataInputView in) throws IOException;

  /**
   * deserialize file name state from serialized bytes
   * first, we will check if the state type is correct to file names state
   *
   * @param fileStateType file name state from serialized bytes
   * @param in            serialized bytes
   * @return deserialized file name state
   * @throws IOException read serialized value error
   */
  public AbstractFileState deserializeV1(FileStateFactory.FileStateType fileStateType, DataInputView in) throws IOException {
    if (!validateFileStateType(fileStateType)) {
      throw new IOException(String.format("Deserialize error! Expected file name type %s and exactly file name type is %s.",
          getFileStateType(), fileStateType));
    }
    return deserializeFileStateV1(in);
  }

  public Map<LinkedHashMap<String, String>, List<Path>> getPartSpecToFilePathMap(
      FileSystemCommitter committer) {
    PartitionDirectoryManager.DirectoryType directoryManager = committer.getDirectoryManager();
    int hourIndex = committer.getHourIndex();
    Map<String, List<String>> pendingFileName = getPendingFileName(
        committer.getJobId(),
        directoryManager,
        hourIndex);
    Map<LinkedHashMap<String, String>, List<Path>> partSpecToFilePathMap = new HashMap<>(pendingFileName.size());
    Path taskPath = getTaskTemporaryPath(committer.getTmpPath(), getCheckpointId(), getTaskId());
    for (Map.Entry<String, List<String>> fileNameEntry : pendingFileName.entrySet()) {
      String partitionString = fileNameEntry.getKey();
      List<String> filenames = fileNameEntry.getValue();
      Path tmpSrcPath = directoryManager.createPartitionDirectionPath(taskPath, hourIndex, partitionString);
      log.debug("Tmp path is {}, partition is {} hour index is {}.", tmpSrcPath.getPath(), partitionString, hourIndex);
      LinkedHashMap<String, String> partSpec = committer.getPartitionColumnSize() > 0 ?
          PartitionPathUtils.extractPartitionSpecFromPath(new Path(taskPath, partitionString))
          : new LinkedHashMap<>();
      partSpec = directoryManager.getPartSpec(partSpec, hourIndex);
      List<Path> filePaths = partSpecToFilePathMap.getOrDefault(partSpec, new ArrayList<>());
      filePaths.addAll(filenames.stream().map(filename -> new Path(tmpSrcPath, filename)).collect(Collectors.toList()));
      partSpecToFilePathMap.put(partSpec, filePaths);
    }
    return partSpecToFilePathMap;
  }
}
