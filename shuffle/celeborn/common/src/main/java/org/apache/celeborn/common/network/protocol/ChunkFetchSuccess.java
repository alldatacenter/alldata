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

import org.apache.celeborn.common.network.buffer.ManagedBuffer;
import org.apache.celeborn.common.network.buffer.NettyManagedBuffer;

/**
 * Response to {@link ChunkFetchRequest} when a chunk exists and has been successfully fetched.
 *
 * <p>Note that the server-side encoding of this messages does NOT include the buffer itself, as
 * this may be written by Netty in a more efficient manner (i.e., zero-copy write). Similarly, the
 * client-side decoding will reuse the Netty ByteBuf as the buffer.
 */
public final class ChunkFetchSuccess extends ResponseMessage {
  public final StreamChunkSlice streamChunkSlice;

  public ChunkFetchSuccess(StreamChunkSlice streamChunkSlice, ManagedBuffer buffer) {
    super(buffer);
    this.streamChunkSlice = streamChunkSlice;
  }

  @Override
  public Type type() {
    return Type.CHUNK_FETCH_SUCCESS;
  }

  @Override
  public int encodedLength() {
    return streamChunkSlice.encodedLength();
  }

  /** Encoding does NOT include 'buffer' itself. See {@link MessageEncoder}. */
  @Override
  public void encode(ByteBuf buf) {
    streamChunkSlice.encode(buf);
  }

  @Override
  public ResponseMessage createFailureResponse(String error) {
    return new ChunkFetchFailure(streamChunkSlice, error);
  }

  public static ChunkFetchSuccess decode(ByteBuf buf) {
    return decode(buf, true);
  }

  public static ChunkFetchSuccess decode(ByteBuf buf, boolean decodeBody) {
    StreamChunkSlice streamChunkSlice = StreamChunkSlice.decode(buf);
    if (decodeBody) {
      NettyManagedBuffer managedBuf = new NettyManagedBuffer(buf);
      return new ChunkFetchSuccess(streamChunkSlice, managedBuf);
    } else {
      return new ChunkFetchSuccess(streamChunkSlice, NettyManagedBuffer.EmptyBuffer);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(streamChunkSlice, body());
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ChunkFetchSuccess) {
      ChunkFetchSuccess o = (ChunkFetchSuccess) other;
      return streamChunkSlice.equals(o.streamChunkSlice) && super.equals(o);
    }
    return false;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("streamChunkId", streamChunkSlice)
        .add("buffer", body())
        .toString();
  }
}
