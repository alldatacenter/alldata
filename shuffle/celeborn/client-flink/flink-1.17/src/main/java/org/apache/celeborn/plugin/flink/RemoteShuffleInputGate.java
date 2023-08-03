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
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.core.memory.MemorySegmentProvider;
import org.apache.flink.metrics.SimpleCounter;
import org.apache.flink.runtime.checkpoint.CheckpointOptions;
import org.apache.flink.runtime.checkpoint.channel.ChannelStateWriter;
import org.apache.flink.runtime.checkpoint.channel.InputChannelInfo;
import org.apache.flink.runtime.checkpoint.channel.ResultSubpartitionInfo;
import org.apache.flink.runtime.clusterframework.types.ResourceID;
import org.apache.flink.runtime.deployment.InputGateDeploymentDescriptor;
import org.apache.flink.runtime.event.TaskEvent;
import org.apache.flink.runtime.executiongraph.IndexRange;
import org.apache.flink.runtime.io.network.ConnectionID;
import org.apache.flink.runtime.io.network.LocalConnectionManager;
import org.apache.flink.runtime.io.network.api.CheckpointBarrier;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.BufferDecompressor;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.runtime.io.network.partition.consumer.BufferOrEvent;
import org.apache.flink.runtime.io.network.partition.consumer.IndexedInputGate;
import org.apache.flink.runtime.io.network.partition.consumer.InputChannel;
import org.apache.flink.runtime.io.network.partition.consumer.RemoteInputChannel;
import org.apache.flink.runtime.io.network.partition.consumer.SingleInputGate;
import org.apache.flink.runtime.jobgraph.IntermediateDataSetID;
import org.apache.flink.runtime.taskmanager.TaskManagerLocation;
import org.apache.flink.runtime.throughput.ThroughputCalculator;
import org.apache.flink.util.CloseableIterator;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.flink.util.clock.SystemClock;
import org.apache.flink.util.function.SupplierWithException;

import org.apache.celeborn.common.CelebornConf;

/** A {@link IndexedInputGate} which ingest data from remote shuffle workers. */
public class RemoteShuffleInputGate extends IndexedInputGate {

  private final RemoteShuffleInputGateDelegation inputGateDelegation;

  public RemoteShuffleInputGate(
      CelebornConf celebornConf,
      String taskName,
      int gateIndex,
      InputGateDeploymentDescriptor gateDescriptor,
      SupplierWithException<BufferPool, IOException> bufferPoolFactory,
      BufferDecompressor bufferDecompressor,
      int numConcurrentReading) {

    inputGateDelegation =
        new RemoteShuffleInputGateDelegation(
            celebornConf,
            taskName,
            gateIndex,
            gateDescriptor,
            bufferPoolFactory,
            bufferDecompressor,
            numConcurrentReading,
            availabilityHelper,
            gateDescriptor.getConsumedSubpartitionIndexRange().getStartIndex(),
            gateDescriptor.getConsumedSubpartitionIndexRange().getEndIndex());
  }

  /** Setup gate and build network connections. */
  @Override
  public void setup() throws IOException {
    inputGateDelegation.setup();
  }

  /** Index of the gate of the corresponding computing task. */
  @Override
  public int getGateIndex() {
    return inputGateDelegation.getGateIndex();
  }

  /** Get number of input channels. A channel is a data flow from one shuffle worker. */
  @Override
  public int getNumberOfInputChannels() {
    return inputGateDelegation.getBufferReaders().size();
  }

  /** Whether reading is finished -- all channels are finished and cached buffers are drained. */
  @Override
  public boolean isFinished() {
    return inputGateDelegation.isFinished();
  }

  @Override
  public Optional<BufferOrEvent> getNext() {
    throw new UnsupportedOperationException("Not implemented (DataSet API is not supported).");
  }

  /** Poll a received {@link BufferOrEvent}. */
  @Override
  public Optional<BufferOrEvent> pollNext() throws IOException {
    return inputGateDelegation.pollNext();
  }

  /** Close all reading channels inside this {@link RemoteShuffleInputGate}. */
  @Override
  public void close() throws Exception {
    inputGateDelegation.close();
  }

  /** Get {@link InputChannelInfo}s of this {@link RemoteShuffleInputGate}. */
  @Override
  public List<InputChannelInfo> getChannelInfos() {
    return inputGateDelegation.getChannelsInfo();
  }

  @Override
  public void requestPartitions() {
    // do-nothing
  }

  @Override
  public void checkpointStarted(CheckpointBarrier barrier) {
    // do-nothing.
  }

  @Override
  public void checkpointStopped(long cancelledCheckpointId) {
    // do-nothing.
  }

  @Override
  public void triggerDebloating() {
    // do-nothing.
  }

  @Override
  public List<InputChannelInfo> getUnfinishedChannels() {
    return Collections.emptyList();
  }

  @Override
  public EndOfDataStatus hasReceivedEndOfData() {
    if (inputGateDelegation.getPendingEndOfDataEvents() > 0) {
      return EndOfDataStatus.NOT_END_OF_DATA;
    } else {
      // Keep compatibility with streaming mode.
      return EndOfDataStatus.DRAINED;
    }
  }

  @Override
  public void finishReadRecoveredState() {
    // do-nothing.
  }

  @Override
  public InputChannel getChannel(int channelIndex) {
    return new FakedRemoteInputChannel(channelIndex);
  }

  @Override
  public void sendTaskEvent(TaskEvent event) {
    throw new FlinkRuntimeException("Method should not be called.");
  }

  @Override
  public void resumeConsumption(InputChannelInfo channelInfo) {
    throw new FlinkRuntimeException("Method should not be called.");
  }

  @Override
  public void acknowledgeAllRecordsProcessed(InputChannelInfo inputChannelInfo) {}

  @Override
  public CompletableFuture<Void> getStateConsumedFuture() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public String toString() {
    return String.format(
        "ReadGate [owning task: %s, gate index: %d, descriptor: %s]",
        inputGateDelegation.getTaskName(),
        inputGateDelegation.getGateIndex(),
        inputGateDelegation.getGateDescriptor().toString());
  }

  /** Accommodation for the incompleteness of Flink pluggable shuffle service. */
  private class FakedRemoteInputChannel extends RemoteInputChannel {
    FakedRemoteInputChannel(int channelIndex) {
      super(
          new SingleInputGate(
              "",
              inputGateDelegation.getGateIndex(),
              new IntermediateDataSetID(),
              ResultPartitionType.BLOCKING,
              new IndexRange(0, 0),
              1,
              (a, b, c) -> {},
              () -> null,
              null,
              new FakedMemorySegmentProvider(),
              0,
              new ThroughputCalculator(SystemClock.getInstance()),
              null),
          channelIndex,
          new ResultPartitionID(),
          0,
          new ConnectionID(
              new TaskManagerLocation(ResourceID.generate(), InetAddress.getLoopbackAddress(), 1),
              0),
          new LocalConnectionManager(),
          0,
          0,
          0,
          new SimpleCounter(),
          new SimpleCounter(),
          new FakedChannelStateWriter());
    }
  }

  /** Accommodation for the incompleteness of Flink pluggable shuffle service. */
  private static class FakedMemorySegmentProvider implements MemorySegmentProvider {

    @Override
    public Collection<MemorySegment> requestUnpooledMemorySegments(int i) throws IOException {
      return null;
    }

    @Override
    public void recycleUnpooledMemorySegments(Collection<MemorySegment> collection)
        throws IOException {}
  }

  /** Accommodation for the incompleteness of Flink pluggable shuffle service. */
  private static class FakedChannelStateWriter implements ChannelStateWriter {

    @Override
    public void start(long cpId, CheckpointOptions checkpointOptions) {}

    @Override
    public void addInputData(
        long cpId, InputChannelInfo info, int startSeqNum, CloseableIterator<Buffer> data) {}

    @Override
    public void addOutputData(
        long cpId, ResultSubpartitionInfo info, int startSeqNum, Buffer... data) {}

    @Override
    public void addOutputDataFuture(
        long l,
        ResultSubpartitionInfo resultSubpartitionInfo,
        int i,
        CompletableFuture<List<Buffer>> completableFuture)
        throws IllegalArgumentException {}

    @Override
    public void finishInput(long checkpointId) {}

    @Override
    public void finishOutput(long checkpointId) {}

    @Override
    public void abort(long checkpointId, Throwable cause, boolean cleanup) {}

    @Override
    public ChannelStateWriteResult getAndRemoveWriteResult(long checkpointId) {
      return null;
    }

    @Override
    public void close() {}
  }
}
