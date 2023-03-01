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

package org.apache.celeborn.plugin.flink.utils;

import static org.apache.celeborn.plugin.flink.utils.Utils.checkArgument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;

import org.apache.celeborn.plugin.flink.buffer.BufferHeader;

/** Utility methods to process flink buffers. */
public class BufferUtils {

  // subpartitionid(4) + attemptId(4) + nextBatchId(4) + compressedsize
  public static final int HEADER_LENGTH_PREFIX = 4 * 4;
  // dataType(1) + isCompress(1) + size(4)
  public static final int HEADER_LENGTH = HEADER_LENGTH_PREFIX + 1 + 1 + 4;

  /**
   * Copies the data of the compressed buffer and the corresponding buffer header to the origin
   * buffer. The origin buffer must reserve the {@link #HEADER_LENGTH} space for the header data.
   */
  public static void setCompressedDataWithHeader(Buffer buffer, Buffer compressedBuffer) {
    checkArgument(buffer != null, "Must be not null.");
    checkArgument(buffer.getReaderIndex() == 0, "Illegal reader index.");

    boolean isCompressed = compressedBuffer != null && compressedBuffer.isCompressed();
    int dataLength =
        isCompressed ? compressedBuffer.readableBytes() : buffer.readableBytes() - HEADER_LENGTH;
    ByteBuf byteBuf = buffer.asByteBuf();
    setBufferHeader(byteBuf, buffer.getDataType(), isCompressed, dataLength);

    if (isCompressed) {
      byteBuf.writeBytes(compressedBuffer.asByteBuf());
    }
    buffer.setSize(dataLength + HEADER_LENGTH);
  }

  public static void setBufferHeader(
      ByteBuf byteBuf, Buffer.DataType dataType, boolean isCompressed, int dataLength) {
    byteBuf.writerIndex(0);
    byteBuf.writeInt(0);
    byteBuf.writeInt(0);
    byteBuf.writeInt(0);
    byteBuf.writeInt(0);
    byteBuf.writeByte(dataType.ordinal());
    byteBuf.writeBoolean(isCompressed);
    byteBuf.writeInt(dataLength);
  }

  public static BufferHeader getBufferHeader(Buffer buffer, int position) {
    return getBufferHeader(buffer, position, false);
  }

  public static BufferHeader getBufferHeader(Buffer buffer, int position, boolean isFirst) {
    ByteBuf byteBuf = buffer.asByteBuf();
    byteBuf.readerIndex(position);
    if (!isFirst) {
      return new BufferHeader(
          Buffer.DataType.values()[byteBuf.readByte()], byteBuf.readBoolean(), byteBuf.readInt());
    } else {
      return new BufferHeader(
          byteBuf.readInt(),
          byteBuf.readInt(),
          byteBuf.readInt(),
          byteBuf.readInt(),
          Buffer.DataType.values()[byteBuf.readByte()],
          byteBuf.readBoolean(),
          byteBuf.readInt());
    }
  }

  public static void reserveNumRequiredBuffers(BufferPool bufferPool, int numRequiredBuffers)
      throws IOException {
    long startTime = System.nanoTime();
    List<MemorySegment> buffers = new ArrayList<>(numRequiredBuffers);
    try {
      // guarantee that we have at least the minimal number of buffers
      while (buffers.size() < numRequiredBuffers) {
        MemorySegment segment = bufferPool.requestMemorySegment();
        if (segment != null) {
          buffers.add(segment);
          continue;
        }

        Thread.sleep(10);
        if ((System.nanoTime() - startTime) > 3L * 60 * 1000_000_000) {
          throw new IOException("Could not allocate the required number of buffers in 3 minutes.");
        }
      }
    } catch (Throwable throwable) {
      throw new IOException(throwable);
    } finally {
      buffers.forEach(bufferPool::recycle);
    }
  }
}
