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

package org.apache.celeborn.plugin.flink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.runtime.event.AbstractEvent;
import org.apache.flink.runtime.io.network.api.EndOfData;
import org.apache.flink.runtime.io.network.api.EndOfPartitionEvent;
import org.apache.flink.runtime.io.network.api.serialization.EventSerializer;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.Buffer.DataType;
import org.apache.flink.runtime.io.network.buffer.BufferCompressor;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.NetworkBuffer;
import org.apache.flink.runtime.io.network.partition.BufferAvailabilityListener;
import org.apache.flink.runtime.io.network.partition.ResultPartition;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionManager;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.runtime.io.network.partition.ResultSubpartitionView;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.function.SupplierWithException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.plugin.flink.buffer.PartitionSortedBuffer;
import org.apache.celeborn.plugin.flink.buffer.SortBuffer;
import org.apache.celeborn.plugin.flink.utils.BufferUtils;
import org.apache.celeborn.plugin.flink.utils.Utils;

/**
 * A {@link ResultPartition} which appends records and events to {@link SortBuffer} and after the
 * {@link SortBuffer} is full, all data in the {@link SortBuffer} will be copied and spilled to the
 * remote shuffle service in subpartition index order sequentially. Large records that can not be
 * appended to an empty {@link org.apache.flink.runtime.io.network.partition.SortBuffer} will be
 * spilled directly.
 */
public class RemoteShuffleResultPartition extends ResultPartition {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteShuffleResultPartition.class);

  /** Size of network buffer and write buffer. */
  private final int networkBufferSize;

  /** {@link SortBuffer} for records sent by {@link #broadcastRecord(ByteBuffer)}. */
  private SortBuffer broadcastSortBuffer;

  /** {@link SortBuffer} for records sent by {@link #emitRecord(ByteBuffer, int)}. */
  private SortBuffer unicastSortBuffer;

  /** Utility to spill data to shuffle workers. */
  private final RemoteShuffleOutputGate outputGate;

  /** Whether {@link #notifyEndOfData()} has been called or not. */
  private boolean endOfDataNotified;

  public RemoteShuffleResultPartition(
      String owningTaskName,
      int partitionIndex,
      ResultPartitionID partitionId,
      ResultPartitionType partitionType,
      int numSubpartitions,
      int numTargetKeyGroups,
      int networkBufferSize,
      ResultPartitionManager partitionManager,
      @Nullable BufferCompressor bufferCompressor,
      SupplierWithException<BufferPool, IOException> bufferPoolFactory,
      RemoteShuffleOutputGate outputGate) {

    super(
        owningTaskName,
        partitionIndex,
        partitionId,
        partitionType,
        numSubpartitions,
        numTargetKeyGroups,
        partitionManager,
        bufferCompressor,
        bufferPoolFactory);

    this.networkBufferSize = networkBufferSize;
    this.outputGate = outputGate;
  }

  @Override
  public void setup() throws IOException {
    LOG.info("Setup {}", this);
    super.setup();
    BufferUtils.reserveNumRequiredBuffers(bufferPool, 1);
    try {
      outputGate.setup();
    } catch (Throwable throwable) {
      LOG.error("Failed to setup remote output gate.", throwable);
      Utils.rethrowAsRuntimeException(throwable);
    }
  }

  @Override
  public void emitRecord(ByteBuffer record, int targetSubpartition) throws IOException {
    emit(record, targetSubpartition, DataType.DATA_BUFFER, false);
  }

  @Override
  public void broadcastRecord(ByteBuffer record) throws IOException {
    broadcast(record, DataType.DATA_BUFFER);
  }

  @Override
  public void broadcastEvent(AbstractEvent event, boolean isPriorityEvent) throws IOException {
    Buffer buffer = EventSerializer.toBuffer(event, isPriorityEvent);
    try {
      ByteBuffer serializedEvent = buffer.getNioBufferReadable();
      broadcast(serializedEvent, buffer.getDataType());
    } finally {
      buffer.recycleBuffer();
    }
  }

  private void broadcast(ByteBuffer record, DataType dataType) throws IOException {
    emit(record, 0, dataType, true);
  }

  private void emit(
      ByteBuffer record, int targetSubpartition, DataType dataType, boolean isBroadcast)
      throws IOException {

    checkInProduceState();
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

  private void releaseSortBuffer(SortBuffer sortBuffer) {
    if (sortBuffer != null) {
      sortBuffer.release();
    }
  }

  @VisibleForTesting
  SortBuffer getUnicastSortBuffer() throws IOException {
    flushBroadcastSortBuffer();

    if (unicastSortBuffer != null && !unicastSortBuffer.isFinished()) {
      return unicastSortBuffer;
    }

    unicastSortBuffer =
        new PartitionSortedBuffer(bufferPool, numSubpartitions, networkBufferSize, null);
    return unicastSortBuffer;
  }

  private SortBuffer getBroadcastSortBuffer() throws IOException {
    flushUnicastSortBuffer();

    if (broadcastSortBuffer != null && !broadcastSortBuffer.isFinished()) {
      return broadcastSortBuffer;
    }

    broadcastSortBuffer =
        new PartitionSortedBuffer(bufferPool, numSubpartitions, networkBufferSize, null);
    return broadcastSortBuffer;
  }

  private void flushBroadcastSortBuffer() throws IOException {
    flushSortBuffer(broadcastSortBuffer, true);
  }

  private void flushUnicastSortBuffer() throws IOException {
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
          updateStatistics(bufferWithChannel.getBuffer());
          writeCompressedBufferIfPossible(buffer, subpartitionIndex);
        }
        outputGate.regionFinish();
      } catch (InterruptedException e) {
        throw new IOException("Failed to flush the sort buffer, broadcast=" + isBroadcast, e);
      }
    }
    releaseSortBuffer(sortBuffer);
  }

  private void writeCompressedBufferIfPossible(Buffer buffer, int targetSubpartition)
      throws InterruptedException {
    Buffer compressedBuffer = null;
    try {
      if (canBeCompressed(buffer)) {
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

  private void updateStatistics(Buffer buffer) {
    numBuffersOut.inc();
    numBytesOut.inc(buffer.readableBytes() - BufferUtils.HEADER_LENGTH);
  }

  /** Spills the large record into {@link RemoteShuffleOutputGate}. */
  private void writeLargeRecord(
      ByteBuffer record, int targetSubpartition, DataType dataType, boolean isBroadcast)
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

      updateStatistics(buffer);
      writeCompressedBufferIfPossible(buffer, targetSubpartition);
    }
    outputGate.regionFinish();
  }

  @Override
  public void finish() throws IOException {
    Utils.checkState(!isReleased(), "Result partition is already released.");
    broadcastEvent(EndOfPartitionEvent.INSTANCE, false);
    Utils.checkState(
        unicastSortBuffer == null || unicastSortBuffer.isReleased(),
        "The unicast sort buffer should be either null or released.");
    flushBroadcastSortBuffer();
    try {
      outputGate.finish();
    } catch (InterruptedException e) {
      throw new IOException("Output gate fails to finish.", e);
    }
    super.finish();
  }

  @Override
  public synchronized void close() {
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
        checkException(() -> super.close(), closeException, "Failed to call super#close() method.");

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

  private Throwable checkException(Runnable runnable, Throwable exception, String errorMessage) {
    Throwable newException = null;
    try {
      runnable.run();
    } catch (Throwable throwable) {
      newException = exception == null ? throwable : exception;
      LOG.error(errorMessage, throwable);
    }
    return newException;
  }

  @Override
  protected void releaseInternal() {
    // no-op
  }

  @Override
  public void flushAll() {
    try {
      flushUnicastSortBuffer();
      flushBroadcastSortBuffer();
    } catch (Throwable t) {
      LOG.error("Failed to flush the current sort buffer.", t);
      Utils.rethrowAsRuntimeException(t);
    }
  }

  @Override
  public void flush(int subpartitionIndex) {
    flushAll();
  }

  @Override
  public CompletableFuture<?> getAvailableFuture() {
    return AVAILABLE;
  }

  @Override
  public int getNumberOfQueuedBuffers() {
    return 0;
  }

  @Override
  public int getNumberOfQueuedBuffers(int targetSubpartition) {
    return 0;
  }

  @Override
  public ResultSubpartitionView createSubpartitionView(
      int index, BufferAvailabilityListener availabilityListener) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public void notifyEndOfData() throws IOException {
    if (!endOfDataNotified) {
      broadcastEvent(EndOfData.INSTANCE, false);
      endOfDataNotified = true;
    }
  }

  @Override
  public CompletableFuture<Void> getAllDataProcessedFuture() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public String toString() {
    return "ResultPartition "
        + partitionId.toString()
        + " ["
        + partitionType
        + ", "
        + numSubpartitions
        + " subpartitions, shuffle-descriptor: "
        + outputGate.getShuffleDesc()
        + "]";
  }
}
