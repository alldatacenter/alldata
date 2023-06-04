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

import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.FileNameUtils;

import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.core.memory.DataInputDeserializer;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileStateSerializer implements SimpleVersionedSerializer<List<AbstractFileState>> {
  private static final int MAGIC_NUMBER = 0x7a2b3cd4;

  public FileStateSerializer() {
  }

  private static void validateMagicNumber(DataInputView in) throws IOException {
    final int magicNumber = in.readInt();
    if (magicNumber != MAGIC_NUMBER) {
      throw new IOException(String.format("Corrupt data: Unexpected magic number %08X", magicNumber));
    }
  }

  @Override
  public int getVersion() {
    return FileNameUtils.SupportedVersion.version1.getVersionNum();
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public byte[] serialize(List<AbstractFileState> state) throws IOException {
    DataOutputSerializer out = new DataOutputSerializer(256);
    out.writeInt(MAGIC_NUMBER);
    serializeV1(state, out);
    return out.getCopyOfBuffer();
  }

  private void serializeV1(List<AbstractFileState> fileNameStates, DataOutputSerializer dataOutputView) throws IOException {
    dataOutputView.writeInt(fileNameStates.size());
    for (AbstractFileState fileNameState : fileNameStates) {
      fileNameState.serializeV1(dataOutputView);
    }
  }

  @Override
  public List<AbstractFileState> deserialize(int version, byte[] bytes) throws IOException {
    final DataInputDeserializer in = new DataInputDeserializer(bytes);
    FileNameUtils.SupportedVersion supportedVersion = FileNameUtils.SupportedVersion.getVersion(version);
    switch (supportedVersion) {
      case version1:
        validateMagicNumber(in);
        return deserializeV1(in);
      default:
        throw new IOException("Unrecognized version or corrupt state: " + version);
    }
  }

  private List<AbstractFileState> deserializeV1(DataInputView in) throws IOException {
    int size = in.readInt();
    List<AbstractFileState> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      result.add(FileStateFactory.deserializeV1(in));
    }
    return result;
  }
}
