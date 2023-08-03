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
import org.apache.flink.runtime.event.AbstractEvent;
import org.apache.flink.runtime.io.network.api.EndOfData;
import org.apache.flink.runtime.io.network.api.EndOfPartitionEvent;
import org.apache.flink.runtime.io.network.api.StopMode;
import org.apache.flink.runtime.io.network.api.serialization.EventSerializer;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.Buffer.DataType;
import org.apache.flink.runtime.io.network.buffer.BufferCompressor;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.partition.*;
import org.apache.flink.util.function.SupplierWithException;

import org.apache.celeborn.plugin.flink.buffer.SortBuffer;
import org.apache.celeborn.plugin.flink.utils.BufferUtils;
import org.apache.celeborn.plugin.flink.utils.Utils;

/**
 * A {@link ResultPartition} which appends records and events to {@link SortBuffer} and after the
 * {@link SortBuffer} is full, all data in the {@link SortBuffer} will be copied and spilled to the
 * remote shuffle service in subpartition index order sequentially. Large records that can not be
 * appended to an empty {@link SortBuffer} will be spilled directly.
 */
public class RemoteShuffleResultPartition extends ResultPartition {

  private final RemoteShuffleResultPartitionDelegation delegation;

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

    delegation =
        new RemoteShuffleResultPartitionDelegation(
            networkBufferSize,
            outputGate,
            (bufferWithChannel, isBroadcast) -> updateStatistics(bufferWithChannel, isBroadcast),
            numSubpartitions);
  }

  @Override
  public void setup() throws IOException {
    super.setup();
    BufferUtils.reserveNumRequiredBuffers(bufferPool, 1);
    delegation.setup(
        bufferPool,
        bufferCompressor,
        buffer -> canBeCompressed(buffer),
        () -> checkInProduceState());
  }

  @Override
  public void emitRecord(ByteBuffer record, int targetSubpartition) throws IOException {
    delegation.emit(record, targetSubpartition, DataType.DATA_BUFFER, false);
  }

  @Override
  public void broadcastRecord(ByteBuffer record) throws IOException {
    delegation.broadcast(record, DataType.DATA_BUFFER);
  }

  @Override
  public void broadcastEvent(AbstractEvent event, boolean isPriorityEvent) throws IOException {
    Buffer buffer = EventSerializer.toBuffer(event, isPriorityEvent);
    try {
      ByteBuffer serializedEvent = buffer.getNioBufferReadable();
      delegation.broadcast(serializedEvent, buffer.getDataType());
    } finally {
      buffer.recycleBuffer();
    }
  }

  @Override
  public void finish() throws IOException {
    Utils.checkState(!isReleased(), "Result partition is already released.");
    broadcastEvent(EndOfPartitionEvent.INSTANCE, false);
    delegation.finish();
    super.finish();
  }

  @Override
  public synchronized void close() {
    delegation.close(() -> super.close());
  }

  @Override
  protected void releaseInternal() {
    // no-op
  }

  @Override
  public void flushAll() {
    delegation.flushAll();
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
  public long getSizeOfQueuedBuffersUnsafe() {
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
  public void notifyEndOfData(StopMode mode) throws IOException {
    if (!delegation.isEndOfDataNotified()) {
      broadcastEvent(new EndOfData(mode), false);
      delegation.setEndOfDataNotified(true);
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
        + delegation.getOutputGate().getShuffleDesc()
        + "]";
  }

  @VisibleForTesting
  public RemoteShuffleResultPartitionDelegation getDelegation() {
    return delegation;
  }

  public void updateStatistics(
      SortBuffer.BufferWithChannel bufferWithChannel, boolean isBroadcast) {
    numBuffersOut.inc(isBroadcast ? numSubpartitions : 1);
    long readableBytes = bufferWithChannel.getBuffer().readableBytes() - BufferUtils.HEADER_LENGTH;
    numBytesProduced.inc(readableBytes);
    numBytesOut.inc(isBroadcast ? readableBytes * numSubpartitions : readableBytes);
  }
}
