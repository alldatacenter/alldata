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

package com.bytedance.bitsail.flink.core.delagate.serializer;

import com.bytedance.bitsail.base.connector.writer.v1.comittable.CommittableState;

import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.core.memory.DataInputDeserializer;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputSerializer;
import org.apache.flink.core.memory.DataOutputView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created 2022/6/17
 */
public class CommittableStateSerializer<CommitT> implements SimpleVersionedSerializer<CommittableState<CommitT>> {

  private static final int MAGIC_NUMBER = 0x88c5d;
  private final SimpleVersionedSerializer<CommitT> committableSerializer;

  public CommittableStateSerializer(SimpleVersionedSerializer<CommitT> committableSerializer) {
    this.committableSerializer = committableSerializer;
  }

  private static void validateMagicNumber(DataInputView in) throws IOException {
    final int magicNumber = in.readInt();
    if (magicNumber != MAGIC_NUMBER) {
      throw new IOException(
          String.format("Corrupt data: Unexpected magic number %08X", magicNumber));
    }
  }

  @Override
  public int getVersion() {
    return this.committableSerializer.getVersion();
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public byte[] serialize(CommittableState<CommitT> state) throws IOException {
    DataOutputSerializer out = new DataOutputSerializer(256);
    out.writeInt(MAGIC_NUMBER);
    serializeV1(state, out);
    return out.getCopyOfBuffer();
  }

  @Override
  public CommittableState<CommitT> deserialize(int version, byte[] serialized)
      throws IOException {
    final DataInputDeserializer in = new DataInputDeserializer(serialized);
    if (version == getVersion()) {
      validateMagicNumber(in);
      return deserializeV1(in);
    }
    throw new IOException("Unrecognized version or corrupt state: " + version);
  }

  private CommittableState<CommitT> deserializeV1(DataInputView in) throws IOException {
    final List<CommitT> committables = new ArrayList<>();
    final int committableSerializerVersion = in.readInt();
    final int numOfCommittable = in.readInt();
    final long checkpointId = in.readLong();

    for (int i = 0; i < numOfCommittable; i++) {
      final byte[] bytes = new byte[in.readInt()];
      in.readFully(bytes);
      final CommitT committable =
          committableSerializer.deserialize(committableSerializerVersion, bytes);
      committables.add(committable);
    }

    return new CommittableState<>(checkpointId, committables);
  }

  private void serializeV1(CommittableState<CommitT> state, DataOutputView dataOutputView)
      throws IOException {

    dataOutputView.writeInt(committableSerializer.getVersion());
    dataOutputView.writeInt(state.getCommittables().size());
    dataOutputView.writeLong(state.getCheckpointId());

    for (CommitT committable : state.getCommittables()) {
      final byte[] serialized = committableSerializer.serialize(committable);
      dataOutputView.writeInt(serialized.length);
      dataOutputView.write(serialized);
    }
  }
}
