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

import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;

/** Encapsulates a request for a particular chunk of a stream. */
public final class StreamChunkSlice implements Encodable {
  public final long streamId;
  public final int chunkIndex;
  /** offset from the beginning of the chunk */
  public final int offset;
  /** size to read */
  public final int len;

  public StreamChunkSlice(long streamId, int chunkIndex) {
    this.streamId = streamId;
    this.chunkIndex = chunkIndex;
    this.offset = 0;
    this.len = Integer.MAX_VALUE;
  }

  public StreamChunkSlice(long streamId, int chunkIndex, int offset, int len) {
    this.streamId = streamId;
    this.chunkIndex = chunkIndex;
    this.offset = offset;
    this.len = len;
  }

  @Override
  public int encodedLength() {
    return 20;
  }

  public void encode(ByteBuf buffer) {
    buffer.writeLong(streamId);
    buffer.writeInt(chunkIndex);
    buffer.writeInt(offset);
    buffer.writeInt(len);
  }

  public static StreamChunkSlice decode(ByteBuf buffer) {
    assert buffer.readableBytes() >= 20;
    long streamId = buffer.readLong();
    int chunkIndex = buffer.readInt();
    int offset = buffer.readInt();
    int len = buffer.readInt();
    return new StreamChunkSlice(streamId, chunkIndex, offset, len);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(streamId, chunkIndex, offset, len);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof StreamChunkSlice) {
      StreamChunkSlice o = (StreamChunkSlice) other;
      return streamId == o.streamId
          && chunkIndex == o.chunkIndex
          && offset == o.offset
          && len == o.len;
    }
    return false;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("streamId", streamId)
        .add("chunkIndex", chunkIndex)
        .add("offset", offset)
        .add("len", len)
        .toString();
  }
}
