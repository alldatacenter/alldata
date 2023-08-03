/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.flink.read.hybrid.enumerator;

import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplitSerializer;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplitState;
import com.netease.arctic.flink.read.hybrid.split.TemporalJoinSplits;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.core.memory.DataInputDeserializer;
import org.apache.flink.core.memory.DataOutputSerializer;
import org.apache.flink.util.InstantiationUtil;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

/**
 * Serializer that serializes and deserializes arctic enumerator {@link ArcticSourceEnumState}.
 */
public class ArcticSourceEnumStateSerializer implements SimpleVersionedSerializer<ArcticSourceEnumState> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArcticSourceEnumStateSerializer.class);
  private static final int VERSION = 1;
  private final ArcticSplitSerializer splitSerializer = ArcticSplitSerializer.INSTANCE;
  private final ArcticEnumeratorOffsetSerializer offsetSerializer = ArcticEnumeratorOffsetSerializer.INSTANCE;

  private static final ThreadLocal<DataOutputSerializer> SERIALIZER_CACHE =
      ThreadLocal.withInitial(() -> new DataOutputSerializer(1024));

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public byte[] serialize(ArcticSourceEnumState arcticSourceEnumState) throws IOException {
    return serializeV1(arcticSourceEnumState);
  }

  private byte[] serializeV1(ArcticSourceEnumState enumState) throws IOException {
    DataOutputSerializer out = SERIALIZER_CACHE.get();

    out.writeBoolean(enumState.lastEnumeratedOffset() != null);
    if (enumState.lastEnumeratedOffset() != null) {
      out.writeInt(offsetSerializer.getVersion());
      byte[] positionBytes = offsetSerializer.serialize(enumState.lastEnumeratedOffset());
      out.writeInt(positionBytes.length);
      out.write(positionBytes);
    }

    out.writeInt(splitSerializer.getVersion());
    out.writeInt(enumState.pendingSplits().size());
    for (ArcticSplitState splitState : enumState.pendingSplits()) {
      byte[] splitBytes = splitSerializer.serialize(splitState.toSourceSplit());
      out.writeInt(splitBytes.length);
      out.write(splitBytes);
    }

    out.writeBoolean(enumState.shuffleSplitRelation() != null);
    if (enumState.shuffleSplitRelation() != null) {
      long[] shuffleSplitRelation = enumState.shuffleSplitRelation();
      out.writeInt(Objects.requireNonNull(shuffleSplitRelation).length);
      for (long l : shuffleSplitRelation) {
        out.writeLong(l);
      }
    }

    out.writeBoolean(enumState.temporalJoinSplits() != null);
    if (enumState.temporalJoinSplits() != null) {
      byte[] temporalJoinSplits = InstantiationUtil.serializeObject(enumState.temporalJoinSplits());
      out.writeInt(temporalJoinSplits.length);
      out.write(temporalJoinSplits);
    }

    byte[] result = out.getCopyOfBuffer();
    out.clear();
    return result;
  }

  @Override
  public ArcticSourceEnumState deserialize(int version, byte[] serialized) throws IOException {
    switch (version) {
      case 1:
        return deserializeV1(serialized);
      default:
        throw new IOException("Unknown version: " + version);
    }
  }

  private ArcticSourceEnumState deserializeV1(byte[] serialized) throws IOException {
    DataInputDeserializer in = new DataInputDeserializer(serialized);

    ArcticEnumeratorOffset enumeratorOffset = null;
    if (in.readBoolean()) {
      int version = in.readInt();
      byte[] positionBytes = new byte[in.readInt()];
      in.read(positionBytes);
      enumeratorOffset = offsetSerializer.deserialize(version, positionBytes);
    }

    int splitSerializerVersion = in.readInt();
    int splitCount = in.readInt();
    Collection<ArcticSplitState> pendingSplits = Lists.newArrayListWithCapacity(splitCount);
    for (int i = 0; i < splitCount; ++i) {
      byte[] splitBytes = new byte[in.readInt()];
      in.read(splitBytes);
      ArcticSplit split = splitSerializer.deserialize(splitSerializerVersion, splitBytes);
      pendingSplits.add(new ArcticSplitState(split));
    }

    long[] shuffleSplitRelation = null;
    if (in.readBoolean()) {
      int length = in.readInt();
      shuffleSplitRelation = new long[length];
      for (int i = 0; i < length; i++) {
        shuffleSplitRelation[i] = in.readLong();
      }
    }

    TemporalJoinSplits temporalJoinSplits = null;
    if (in.readBoolean()) {
      byte[] bytes = new byte[in.readInt()];
      in.read(bytes);
      try {
        temporalJoinSplits = InstantiationUtil.deserializeObject(bytes, TemporalJoinSplits.class.getClassLoader());
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("deserialize FirstSplit error", e);
      }
    }

    return new ArcticSourceEnumState(pendingSplits, enumeratorOffset, shuffleSplitRelation, temporalJoinSplits);
  }
}
