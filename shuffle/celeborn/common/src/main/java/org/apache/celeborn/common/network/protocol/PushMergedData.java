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

package org.apache.celeborn.common.network.protocol;

import java.util.Arrays;

import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;

import org.apache.celeborn.common.network.buffer.ManagedBuffer;
import org.apache.celeborn.common.network.buffer.NettyManagedBuffer;

public final class PushMergedData extends RequestMessage {
  public long requestId;

  // 0 for primary, 1 for replica, see PartitionLocation.Mode
  public final byte mode;

  public final String shuffleKey;
  public final String[] partitionUniqueIds;
  public final int[] batchOffsets;

  public PushMergedData(
      byte mode, String shuffleKey, String[] partitionIds, int[] batchOffsets, ManagedBuffer body) {
    this(0L, mode, shuffleKey, partitionIds, batchOffsets, body);
  }

  private PushMergedData(
      long requestId,
      byte mode,
      String shuffleKey,
      String[] partitionUniqueIds,
      int[] batchOffsets,
      ManagedBuffer body) {
    super(body);
    this.requestId = requestId;
    this.mode = mode;
    this.shuffleKey = shuffleKey;
    this.partitionUniqueIds = partitionUniqueIds;
    this.batchOffsets = batchOffsets;
  }

  @Override
  public Type type() {
    return Type.PUSH_MERGED_DATA;
  }

  @Override
  public int encodedLength() {
    return 8
        + 1
        + Encoders.Strings.encodedLength(shuffleKey)
        + Encoders.StringArrays.encodedLength(partitionUniqueIds)
        + Encoders.IntArrays.encodedLength(batchOffsets);
  }

  @Override
  public void encode(ByteBuf buf) {
    buf.writeLong(requestId);
    buf.writeByte(mode);
    Encoders.Strings.encode(buf, shuffleKey);
    Encoders.StringArrays.encode(buf, partitionUniqueIds);
    Encoders.IntArrays.encode(buf, batchOffsets);
  }

  public static PushMergedData decode(ByteBuf buf) {
    return decode(buf, true);
  }

  public static PushMergedData decode(ByteBuf buf, boolean decodeBody) {
    long requestId = buf.readLong();
    byte mode = buf.readByte();
    String shuffleKey = Encoders.Strings.decode(buf);
    String[] partitionIds = Encoders.StringArrays.decode(buf);
    int[] batchOffsets = Encoders.IntArrays.decode(buf);
    if (decodeBody) {
      return new PushMergedData(
          requestId, mode, shuffleKey, partitionIds, batchOffsets, new NettyManagedBuffer(buf));
    } else {
      return new PushMergedData(
          requestId, mode, shuffleKey, partitionIds, batchOffsets, NettyManagedBuffer.EmptyBuffer);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(requestId, mode, shuffleKey, partitionUniqueIds, batchOffsets, body());
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof PushMergedData) {
      PushMergedData o = (PushMergedData) other;
      return requestId == o.requestId
          && mode == o.mode
          && shuffleKey.equals(o.shuffleKey)
          && Arrays.equals(partitionUniqueIds, o.partitionUniqueIds)
          && Arrays.equals(batchOffsets, o.batchOffsets)
          && super.equals(o);
    }
    return false;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("requestId", requestId)
        .add("mode", mode)
        .add("shuffleKey", shuffleKey)
        .add("partitionIds", Arrays.toString(partitionUniqueIds))
        .add("batchOffsets", Arrays.toString(batchOffsets))
        .add("body size", body().size())
        .toString();
  }
}
