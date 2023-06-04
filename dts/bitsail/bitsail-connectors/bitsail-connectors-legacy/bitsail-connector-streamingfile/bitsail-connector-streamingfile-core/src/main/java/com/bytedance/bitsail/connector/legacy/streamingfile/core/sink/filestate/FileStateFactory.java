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

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.core.memory.DataInputView;

import java.io.IOException;
import java.util.List;

@Slf4j
public class FileStateFactory {
  private final FileStateType fileStateType;
  private final String compressionExtension;
  private final int taskId;

  public FileStateFactory(int taskId, FileStateType fileStateType, String compressionExtension) {
    this.fileStateType = fileStateType;
    this.compressionExtension = compressionExtension;
    this.taskId = taskId;
  }

  public static AbstractFileState deserializeV1(DataInputView in) throws IOException {
    byte fileNameStateIndex = in.readByte();
    FileStateType fileStateType =
        FileStateType.getFileStateType(fileNameStateIndex);
    switch (fileStateType) {
      case TIMESTAMP:
        return new FileStateBasedOnTimestamp(fileStateType, in);
      case FILE_NAME:
      default:
        return new FileStateBasedOnName(fileStateType, in);
    }
  }

  public AbstractFileState buildFileNameState(long nextCheckpointId, List<PartFileInfo> partFileInfos) {
    AbstractFileState abstractFileState;
    switch (fileStateType) {
      case TIMESTAMP:
        abstractFileState = FileStateBasedOnTimestamp.builder()
            .taskId(this.taskId)
            .checkpointId(nextCheckpointId)
            .compressionExtension(this.compressionExtension)
            .build();
        break;
      case FILE_NAME:
      default:
        abstractFileState = FileStateBasedOnName.builder()
            .taskId(this.taskId)
            .checkpointId(nextCheckpointId)
            .build();
    }
    abstractFileState.setCurrentState(partFileInfos);
    return abstractFileState;
  }

  public enum FileStateType {
    FILE_NAME(0),
    TIMESTAMP(1);

    @Getter
    private final byte index;

    FileStateType(Integer index) {
      this.index = index.byteValue();
    }

    public static FileStateType getFileStateType(byte index) {
      for (FileStateType fileStateType : FileStateType.values()) {
        if (fileStateType.getIndex() == index) {
          return fileStateType;
        }
      }
      throw new RuntimeException("Unknown file name state type " + index);
    }
  }
}
