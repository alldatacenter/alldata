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

/** Response to {@link ChunkFetchRequest} when there is an error fetching the chunk. */
public final class ChunkFetchFailure extends ResponseMessage {
  public final StreamChunkSlice streamChunkSlice;
  public final String errorString;

  public ChunkFetchFailure(StreamChunkSlice streamChunkSlice, String errorString) {
    this.streamChunkSlice = streamChunkSlice;
    this.errorString = errorString;
  }

  @Override
  public Type type() {
    return Type.CHUNK_FETCH_FAILURE;
  }

  @Override
  public int encodedLength() {
    return streamChunkSlice.encodedLength() + Encoders.Strings.encodedLength(errorString);
  }

  @Override
  public void encode(ByteBuf buf) {
    streamChunkSlice.encode(buf);
    Encoders.Strings.encode(buf, errorString);
  }

  public static ChunkFetchFailure decode(ByteBuf buf) {
    StreamChunkSlice streamChunkSlice = StreamChunkSlice.decode(buf);
    String errorString = Encoders.Strings.decode(buf);
    return new ChunkFetchFailure(streamChunkSlice, errorString);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(streamChunkSlice, errorString);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ChunkFetchFailure) {
      ChunkFetchFailure o = (ChunkFetchFailure) other;
      return streamChunkSlice.equals(o.streamChunkSlice) && errorString.equals(o.errorString);
    }
    return false;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("streamChunkId", streamChunkSlice)
        .add("errorString", errorString)
        .toString();
  }
}
