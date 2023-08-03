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

package org.apache.celeborn.plugin.flink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.BufferCompressor;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.NetworkBuffer;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.plugin.flink.buffer.PartitionSortedBuffer;
import org.apache.celeborn.plugin.flink.buffer.SortBuffer;
import org.apache.celeborn.plugin.flink.utils.BufferUtils;
import org.apache.celeborn.plugin.flink.utils.Utils;

public class RemoteShuffleResultPartitionDelegation {
  public static final Logger LOG =
      LoggerFactory.getLogger(RemoteShuffleResultPartitionDelegation.class);

  /** Size of network buffer and write buffer. */
  public int networkBufferSize;

  /** {@link SortBuffer} for records sent by broadcastRecord. */
  public SortBuffer broadcastSortBuffer;

  /** {@link SortBuffer} for records sent by emitRecord. */
  public SortBuffer unicastSortBuffer;

  /** Utility to spill data to shuffle workers. */
  public RemoteShuffleOutputGate outputGate;

  /** Whether notifyEndOfData has been called or not. */
  private boolean endOfDataNotified;

  private int numSubpartitions;
  private BufferPool bufferPool;
  private BufferCompressor bufferCompressor;
  private Function<Buffer, Boolean> canBeCompressed;
  private Runnable checkProducerState;
  private BiConsumer<SortBuffer.BufferWithChannel, Boolean> statisticsConsumer;

  public RemoteShuffleResultPartitionDelegation(
      int networkBufferSize,
      RemoteShuffleOutputGate outputGate,
      BiConsumer<SortBuffer.BufferWithChannel, Boolean> statisticsConsumer,
      int numSubpartitions) {
    this.networkBufferSize = networkBufferSize;
    this.outputGate = outputGate;
    this.numSubpartitions = numSubpartitions;
    this.statisticsConsumer = statisticsConsumer;
  }

  public void setup(
      BufferPool bufferPool,
      BufferCompressor bufferCompressor,
      Function<Buffer, Boolean> canBeCompressed,
      Runnable checkProduceState)
      throws IOException {
    LOG.info("Setup {}", this);
    this.bufferPool = bufferPool;
    this.bufferCompressor = bufferCompressor;
    this.canBeCompressed = canBeCompressed;
    this.checkProducerState = checkProduceState;
    try {
      outputGate.setup();
    } catch (Throwable throwable) {
      LOG.error("Failed to setup remote output gate.", throwable);
      Utils.rethrowAsRuntimeException(throwable);
    }
  }

  public void emit(
      ByteBuffer record, int targetSubpartition, Buffer.DataType dataType, boolean isBroadcast)
      throws IOException {

    checkProducerState.run();
    if (isBroadcast) {
      Preconditions.checkState(
          targetSubpartition == 0, "Target subpartition index can only be 0 when broadcast.");
    }

    SortBuffer sortBuffer = isBroadcast ? getBroadcastSortBuffer() : getUnicastSortBuffer();
    if (sortBuffer.append(record, targetSubpartition, dataType)) {
      return;
    }

    try {
      if (!sortBuffer.hasRemaining()) {
        // the record can not be appended to the free sort buffer because it is too large
        sortBuffer.finish();
        sortBuffer.release();
        writeLargeRecord(record, targetSubpartition, dataType, isBroadcast);
        return;
      }
      flushSortBuffer(sortBuffer, isBroadcast);
    } catch (InterruptedException e) {
      LOG.error("Failed to flush the sort buffer.", e);
      Utils.rethrowAsRuntimeException(e);
    }
    emit(record, targetSubpartition, dataType, isBroadcast);
  }

  @VisibleForTesting
  public SortBuffer getUnicastSortBuffer() throws IOException {
    flushBroadcastSortBuffer();

    if (unicastSortBuffer != null && !unicastSortBuffer.isFinished()) {
      return unicastSortBuffer;
    }

    unicastSortBuffer =
        new PartitionSortedBuffer(bufferPool, numSubpartitions, networkBufferSize, null);
    return unicastSortBuffer;
  }

  public SortBuffer getBroadcastSortBuffer() throws IOException {
    flushUnicastSortBuffer();

    if (broadcastSortBuffer != null && !broadcastSortBuffer.isFinished()) {
      return broadcastSortBuffer;
    }

    broadcastSortBuffer =
        new PartitionSortedBuffer(bufferPool, numSubpartitions, networkBufferSize, null);
    return broadcastSortBuffer;
  }

  public void flushBroadcastSortBuffer() throws IOException {
    flushSortBuffer(broadcastSortBuffer, true);
  }

  public void flushUnicastSortBuffer() throws IOException {
    flushSortBuffer(unicastSortBuffer, false);
  }

  @VisibleForTesting
  void flushSortBuffer(SortBuffer sortBuffer, boolean isBroadcast) throws IOException {
    if (sortBuffer == null || sortBuffer.isReleased()) {
      return;
    }
    sortBuffer.finish();
    if (sortBuffer.hasRemaining()) {
      try {
        outputGate.regionStart(isBroadcast);
        while (sortBuffer.hasRemaining()) {
          MemorySegment segment = outputGate.getBufferPool().requestMemorySegmentBlocking();
          SortBuffer.BufferWithChannel bufferWithChannel;
          try {
            bufferWithChannel =
                sortBuffer.copyIntoSegment(
                    segment, outputGate.getBufferPool(), BufferUtils.HEADER_LENGTH);
          } catch (Throwable t) {
            outputGate.getBufferPool().recycle(segment);
            throw new FlinkRuntimeException("Shuffle write failure.", t);
          }

          Buffer buffer = bufferWithChannel.getBuffer();
          int subpartitionIndex = bufferWithChannel.getChannelIndex();
          statisticsConsumer.accept(bufferWithChannel, isBroadcast);
          writeCompressedBufferIfPossible(buffer, subpartitionIndex);
        }
        outputGate.regionFinish();
      } catch (InterruptedException e) {
        throw new IOException("Failed to flush the sort buffer, broadcast=" + isBroadcast, e);
      }
    }
    releaseSortBuffer(sortBuffer);
  }

  public void writeCompressedBufferIfPossible(Buffer buffer, int targetSubpartition)
      throws InterruptedException {
    Buffer compressedBuffer = null;
    try {
      if (canBeCompressed.apply(buffer)) {
        Buffer dataBuffer =
            buffer.readOnlySlice(
                BufferUtils.HEADER_LENGTH, buffer.getSize() - BufferUtils.HEADER_LENGTH);
        compressedBuffer =
            Utils.checkNotNull(bufferCompressor).compressToIntermediateBuffer(dataBuffer);
      }
      BufferUtils.setCompressedDataWithHeader(buffer, compressedBuffer);
    } catch (Throwable throwable) {
      buffer.recycleBuffer();
      throw new RuntimeException("Shuffle write failure.", throwable);
    } finally {
      if (compressedBuffer != null && compressedBuffer.isCompressed()) {
        compressedBuffer.setReaderIndex(0);
        compressedBuffer.recycleBuffer();
      }
    }
    outputGate.write(buffer, targetSubpartition);
  }

  /** Spills the large record into {@link RemoteShuffleOutputGate}. */
  public void writeLargeRecord(
      ByteBuffer record, int targetSubpartition, Buffer.DataType dataType, boolean isBroadcast)
      throws InterruptedException {

    outputGate.regionStart(isBroadcast);
    while (record.hasRemaining()) {
      MemorySegment writeBuffer = outputGate.getBufferPool().requestMemorySegmentBlocking();
      int toCopy = Math.min(record.remaining(), writeBuffer.size() - BufferUtils.HEADER_LENGTH);
      writeBuffer.put(BufferUtils.HEADER_LENGTH, record, toCopy);
      NetworkBuffer buffer =
          new NetworkBuffer(
              writeBuffer,
              outputGate.getBufferPool(),
              dataType,
              toCopy + BufferUtils.HEADER_LENGTH);

      SortBuffer.BufferWithChannel bufferWithChannel =
          new SortBuffer.BufferWithChannel(buffer, targetSubpartition);
      statisticsConsumer.accept(bufferWithChannel, isBroadcast);
      writeCompressedBufferIfPossible(buffer, targetSubpartition);
    }
    outputGate.regionFinish();
  }

  public void broadcast(ByteBuffer record, Buffer.DataType dataType) throws IOException {
    emit(record, 0, dataType, true);
  }

  public void releaseSortBuffer(SortBuffer sortBuffer) {
    if (sortBuffer != null) {
      sortBuffer.release();
    }
  }

  public void finish() throws IOException {
    Utils.checkState(
        unicastSortBuffer == null || unicastSortBuffer.isReleased(),
        "The unicast sort buffer should be either null or released.");
    flushBroadcastSortBuffer();
    try {
      outputGate.finish();
    } catch (InterruptedException e) {
      throw new IOException("Output gate fails to finish.", e);
    }
  }

  public synchronized void close(Runnable closeHandler) {
    Throwable closeException = null;
    closeException =
        checkException(
            () -> releaseSortBuffer(unicastSortBuffer),
            closeException,
            "Failed to release unicast sort buffer.");

    closeException =
        checkException(
            () -> releaseSortBuffer(broadcastSortBuffer),
            closeException,
            "Failed to release broadcast sort buffer.");

    closeException =
        checkException(
            () -> closeHandler.run(), closeException, "Failed to call super#close() method.");

    try {
      outputGate.close();
    } catch (Throwable throwable) {
      closeException = closeException == null ? throwable : closeException;
      LOG.error("Failed to close remote shuffle output gate.", throwable);
    }

    if (closeException != null) {
      Utils.rethrowAsRuntimeException(closeException);
    }
  }

  public Throwable checkException(Runnable runnable, Throwable exception, String errorMessage) {
    Throwable newException = null;
    try {
      runnable.run();
    } catch (Throwable throwable) {
      newException = exception == null ? throwable : exception;
      LOG.error(errorMessage, throwable);
    }
    return newException;
  }

  public void flushAll() {
    try {
      flushUnicastSortBuffer();
      flushBroadcastSortBuffer();
    } catch (Throwable t) {
      LOG.error("Failed to flush the current sort buffer.", t);
      Utils.rethrowAsRuntimeException(t);
    }
  }

  public RemoteShuffleOutputGate getOutputGate() {
    return outputGate;
  }

  public boolean isEndOfDataNotified() {
    return endOfDataNotified;
  }

  public void setEndOfDataNotified(boolean endOfDataNotified) {
    this.endOfDataNotified = endOfDataNotified;
  }
}
