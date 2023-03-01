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

import static org.apache.celeborn.plugin.flink.utils.Utils.checkState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.core.memory.MemorySegmentProvider;
import org.apache.flink.metrics.SimpleCounter;
import org.apache.flink.runtime.checkpoint.CheckpointOptions;
import org.apache.flink.runtime.checkpoint.channel.ChannelStateWriter;
import org.apache.flink.runtime.checkpoint.channel.InputChannelInfo;
import org.apache.flink.runtime.checkpoint.channel.ResultSubpartitionInfo;
import org.apache.flink.runtime.deployment.InputGateDeploymentDescriptor;
import org.apache.flink.runtime.event.AbstractEvent;
import org.apache.flink.runtime.event.TaskEvent;
import org.apache.flink.runtime.io.network.ConnectionID;
import org.apache.flink.runtime.io.network.LocalConnectionManager;
import org.apache.flink.runtime.io.network.api.CheckpointBarrier;
import org.apache.flink.runtime.io.network.api.EndOfData;
import org.apache.flink.runtime.io.network.api.EndOfPartitionEvent;
import org.apache.flink.runtime.io.network.api.serialization.EventSerializer;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.BufferDecompressor;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.partition.PartitionNotFoundException;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.runtime.io.network.partition.consumer.*;
import org.apache.flink.runtime.jobgraph.IntermediateDataSetID;
import org.apache.flink.runtime.shuffle.ShuffleDescriptor;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.apache.flink.util.CloseableIterator;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.flink.util.function.SupplierWithException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.plugin.flink.buffer.BufferPacker;
import org.apache.celeborn.plugin.flink.buffer.TransferBufferPool;
import org.apache.celeborn.plugin.flink.readclient.FlinkShuffleClientImpl;
import org.apache.celeborn.plugin.flink.utils.BufferUtils;

/** A {@link IndexedInputGate} which ingest data from remote shuffle workers. */
public class RemoteShuffleInputGate extends IndexedInputGate {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteShuffleInputGate.class);

  /** Lock to protect {@link #receivedBuffers} and {@link #cause} and {@link #closed}. */
  private final Object lock = new Object();

  /** Name of the corresponding computing task. */
  private final String taskName;

  /** Index of the gate of the corresponding computing task. */
  private final int gateIndex;

  /** Deployment descriptor for a single input gate instance. */
  private final InputGateDeploymentDescriptor gateDescriptor;

  /** Buffer pool provider. */
  private final SupplierWithException<BufferPool, IOException> bufferPoolFactory;

  /** Flink buffer pools to allocate network memory. */
  private BufferPool bufferPool;

  /** Buffer pool used by the transfer layer. */
  private final TransferBufferPool transferBufferPool =
      new TransferBufferPool(Collections.emptySet());

  private final List<RemoteBufferStreamReader> bufferReaders = new ArrayList<>();
  private final List<InputChannelInfo> channelsInfo;
  /** Map from channel index to shuffle client index. */
  private final int[] clientIndexMap;

  /** Map from shuffle client index to channel index. */
  private final int[] channelIndexMap;

  /** The number of subpartitions that has not consumed per channel. */
  private final int[] numSubPartitionsHasNotConsumed;

  /** The overall number of subpartitions that has not been consumed. */
  private long numUnconsumedSubpartitions;

  /** Received buffers from remote shuffle worker. It's consumed by upper computing task. */
  private final Queue<Pair<Buffer, InputChannelInfo>> receivedBuffers = new LinkedList<>();

  /** {@link Throwable} when reading failure. */
  private Throwable cause;

  /** Whether this remote input gate has been closed or not. */
  private boolean closed;

  /** Whether we have opened all initial channels or not. */
  private boolean initialChannelsOpened;

  /** Number of pending {@link EndOfData} events to be received. */
  private long pendingEndOfDataEvents;
  /** Max concurrent reader count */
  private int numConcurrentReading = Integer.MAX_VALUE;
  /** Keep compatibility with streaming mode. */
  private boolean shouldDrainOnEndOfData = true;

  /** Data decompressor. */
  private final BufferDecompressor bufferDecompressor;

  private FlinkShuffleClientImpl shuffleClient;

  public RemoteShuffleInputGate(
      CelebornConf celebornConf,
      String taskName,
      int gateIndex,
      InputGateDeploymentDescriptor gateDescriptor,
      SupplierWithException<BufferPool, IOException> bufferPoolFactory,
      BufferDecompressor bufferDecompressor) {

    this.taskName = taskName;
    this.gateIndex = gateIndex;
    this.gateDescriptor = gateDescriptor;
    this.bufferPoolFactory = bufferPoolFactory;

    int numChannels = gateDescriptor.getShuffleDescriptors().length;
    this.clientIndexMap = new int[numChannels];
    this.channelIndexMap = new int[numChannels];
    this.numSubPartitionsHasNotConsumed = new int[numChannels];
    this.bufferDecompressor = bufferDecompressor;

    RemoteShuffleDescriptor remoteShuffleDescriptor =
        (RemoteShuffleDescriptor) gateDescriptor.getShuffleDescriptors()[0];
    this.shuffleClient =
        FlinkShuffleClientImpl.get(
            remoteShuffleDescriptor.getShuffleResource().getRssMetaServiceHost(),
            remoteShuffleDescriptor.getShuffleResource().getRssMetaServicePort(),
            celebornConf,
            new UserIdentifier("default", "default"));

    this.numUnconsumedSubpartitions = initShuffleReadClients();
    this.pendingEndOfDataEvents = numUnconsumedSubpartitions;
    this.channelsInfo = createChannelInfos();
  }

  private long initShuffleReadClients() {
    int startSubIdx = gateDescriptor.getConsumedSubpartitionIndex();
    int endSubIdx = gateDescriptor.getConsumedSubpartitionIndex();
    int numSubpartitionsPerChannel = endSubIdx - startSubIdx + 1;
    long numUnconsumedSubpartitions = 0;

    // left element is index
    List<Pair<Integer, ShuffleDescriptor>> descriptors =
        IntStream.range(0, gateDescriptor.getShuffleDescriptors().length)
            .mapToObj(i -> Pair.of(i, gateDescriptor.getShuffleDescriptors()[i]))
            .collect(Collectors.toList());

    int clientIndex = 0;
    for (Pair<Integer, ShuffleDescriptor> descriptor : descriptors) {
      RemoteShuffleDescriptor remoteDescriptor = (RemoteShuffleDescriptor) descriptor.getRight();
      ShuffleResourceDescriptor shuffleDescriptor =
          remoteDescriptor.getShuffleResource().getMapPartitionShuffleDescriptor();

      LOG.debug("create shuffle reader for descriptor {}", shuffleDescriptor);
      String applicationId = remoteDescriptor.getCelebornAppId();

      RemoteBufferStreamReader reader =
          new RemoteBufferStreamReader(
              shuffleClient,
              shuffleDescriptor,
              applicationId,
              startSubIdx,
              endSubIdx,
              transferBufferPool,
              getDataListener(descriptor.getLeft()),
              getFailureListener(remoteDescriptor.getResultPartitionID()));

      bufferReaders.add(reader);
      numSubPartitionsHasNotConsumed[descriptor.getLeft()] = numSubpartitionsPerChannel;
      numUnconsumedSubpartitions += numSubpartitionsPerChannel;
      clientIndexMap[descriptor.getLeft()] = clientIndex;
      channelIndexMap[clientIndex] = descriptor.getLeft();
      ++clientIndex;
    }
    return numUnconsumedSubpartitions;
  }

  /** Setup gate and build network connections. */
  @Override
  public void setup() throws IOException {
    long startTime = System.nanoTime();

    bufferPool = bufferPoolFactory.get();
    BufferUtils.reserveNumRequiredBuffers(bufferPool, 16);

    tryRequestBuffers();
    // Complete availability future though handshake not fired yet, thus to allow fetcher to
    // 'pollNext' and fire handshake to remote. This mechanism is to avoid bookkeeping remote
    // reading resource before task start processing data from input gate.
    availabilityHelper.getUnavailableToResetAvailable().complete(null);
    LOG.info("Set up read gate by {} ms.", (System.nanoTime() - startTime) / 1000_000);
  }

  /** Index of the gate of the corresponding computing task. */
  @Override
  public int getGateIndex() {
    return gateIndex;
  }

  /** Get number of input channels. A channel is a data flow from one shuffle worker. */
  @Override
  public int getNumberOfInputChannels() {
    return bufferReaders.size();
  }

  /** Whether reading is finished -- all channels are finished and cached buffers are drained. */
  @Override
  public boolean isFinished() {
    synchronized (lock) {
      return allReadersEOF() && receivedBuffers.isEmpty();
    }
  }

  @Override
  public Optional<BufferOrEvent> getNext() {
    throw new UnsupportedOperationException("Not implemented (DataSet API is not supported).");
  }

  /** Poll a received {@link BufferOrEvent}. */
  @Override
  public Optional<BufferOrEvent> pollNext() throws IOException {
    if (!initialChannelsOpened) {
      tryOpenSomeChannels();
      initialChannelsOpened = true;
      // DO NOT return, method of 'getReceived' will manipulate 'availabilityHelper'.
    }

    Pair<Buffer, InputChannelInfo> pair = getReceived();
    Optional<BufferOrEvent> bufferOrEvent = Optional.empty();
    LOG.debug("pollNext called with pair null {}", pair == null);
    while (pair != null) {
      Buffer buffer = pair.getLeft();
      InputChannelInfo channelInfo = pair.getRight();
      LOG.debug("get buffer {} on channel {}", buffer, channelInfo);
      if (buffer.isBuffer()) {
        bufferOrEvent = transformBuffer(buffer, channelInfo);
      } else {
        bufferOrEvent = transformEvent(buffer, channelInfo);
        LOG.info("recevied event: " + bufferOrEvent.get().getEvent().getClass().getName());
      }

      if (bufferOrEvent.isPresent()) {
        break;
      }
      pair = getReceived();
    }

    tryRequestBuffers();
    return bufferOrEvent;
  }

  private Buffer decompressBufferIfNeeded(Buffer buffer) throws IOException {
    if (buffer.isCompressed()) {
      try {
        checkState(bufferDecompressor != null, "Buffer decompressor not set.");
        return bufferDecompressor.decompressToIntermediateBuffer(buffer);
      } catch (Throwable t) {
        throw new IOException("Decompress failure", t);
      } finally {
        buffer.recycleBuffer();
      }
    }
    return buffer;
  }

  /** Close all reading channels inside this {@link RemoteShuffleInputGate}. */
  @Override
  public void close() throws Exception {
    List<Buffer> buffersToRecycle;
    Throwable closeException = null;
    // Do not check closed flag, thus to allow calling this method from both task thread and
    // cancel thread.
    for (RemoteBufferStreamReader shuffleReadClient : bufferReaders) {
      try {
        shuffleReadClient.close();
      } catch (Throwable throwable) {
        closeException = closeException == null ? throwable : closeException;
        LOG.error("Failed to close shuffle read client.", throwable);
      }
    }
    synchronized (lock) {
      buffersToRecycle = receivedBuffers.stream().map(Pair::getLeft).collect(Collectors.toList());
      receivedBuffers.clear();
      closed = true;
    }

    try {
      buffersToRecycle.forEach(Buffer::recycleBuffer);
    } catch (Throwable throwable) {
      closeException = closeException == null ? throwable : closeException;
      LOG.error("Failed to recycle buffers.", throwable);
    }

    try {
      transferBufferPool.destroy();
    } catch (Throwable throwable) {
      closeException = closeException == null ? throwable : closeException;
      LOG.error("Failed to close transfer buffer pool.", throwable);
    }

    try {
      if (bufferPool != null) {
        bufferPool.lazyDestroy();
      }
    } catch (Throwable throwable) {
      closeException = closeException == null ? throwable : closeException;
      LOG.error("Failed to close local buffer pool.", throwable);
    }

    if (closeException != null) {
      ExceptionUtils.rethrowException(closeException);
    }
  }

  /** Get {@link InputChannelInfo}s of this {@link RemoteShuffleInputGate}. */
  @Override
  public List<InputChannelInfo> getChannelInfos() {
    return channelsInfo;
  }

  /** Each one corresponds to a reading channel. */
  public List<RemoteBufferStreamReader> getBufferReaders() {
    return bufferReaders;
  }

  private List<InputChannelInfo> createChannelInfos() {
    return IntStream.range(0, gateDescriptor.getShuffleDescriptors().length)
        .mapToObj(i -> new InputChannelInfo(gateIndex, i))
        .collect(Collectors.toList());
  }

  /** Try to open more readers to {@link #numConcurrentReading}. */
  private void tryOpenSomeChannels() throws IOException {
    List<RemoteBufferStreamReader> clientsToOpen = new ArrayList<>();

    synchronized (lock) {
      if (closed) {
        throw new IOException("Input gate already closed.");
      }

      LOG.debug("Try open some partition readers.");
      int numOnGoing = 0;
      for (int i = 0; i < bufferReaders.size(); i++) {
        RemoteBufferStreamReader bufferStreamReader = bufferReaders.get(i);
        LOG.debug(
            "Trying reader: {}, isOpened={}, numSubPartitionsHasNotConsumed={}.",
            bufferStreamReader,
            bufferStreamReader.isOpened(),
            numSubPartitionsHasNotConsumed[channelIndexMap[i]]);
        if (numOnGoing >= numConcurrentReading) {
          break;
        }

        if (bufferStreamReader.isOpened()
            && numSubPartitionsHasNotConsumed[channelIndexMap[i]] > 0) {
          numOnGoing++;
          continue;
        }

        if (!bufferStreamReader.isOpened()) {
          clientsToOpen.add(bufferStreamReader);
          numOnGoing++;
        }
      }
    }

    for (RemoteBufferStreamReader reader : clientsToOpen) {
      reader.open(0);
    }
  }

  private void tryRequestBuffers() {
    checkState(bufferPool != null, "Not initialized yet.");

    Buffer buffer;
    List<ByteBuf> buffers = new ArrayList<>();
    while ((buffer = bufferPool.requestBuffer()) != null) {
      buffers.add(buffer.asByteBuf());
    }

    if (!buffers.isEmpty()) {
      transferBufferPool.addBuffers(buffers);
    }
  }

  private void onBuffer(Buffer buffer, int channelIdx) {
    synchronized (lock) {
      if (closed || cause != null) {
        buffer.recycleBuffer();
        throw new IllegalStateException("Input gate already closed or failed.");
      }

      boolean needRecycle = true;
      try {
        boolean wasEmpty = receivedBuffers.isEmpty();
        InputChannelInfo channelInfo = channelsInfo.get(channelIdx);
        checkState(channelInfo.getInputChannelIdx() == channelIdx, "Illegal channel index.");
        LOG.debug("ReceivedBuffers is adding buffer {} on {}", buffer, channelInfo);
        receivedBuffers.add(Pair.of(buffer, channelInfo));
        needRecycle = false;
        if (wasEmpty) {
          availabilityHelper.getUnavailableToResetAvailable().complete(null);
        }
      } catch (Throwable throwable) {
        if (needRecycle) {
          buffer.recycleBuffer();
        }
        throw throwable;
      }
    }
  }

  private Consumer<ByteBuf> getDataListener(int channelIdx) {
    return byteBuf -> {
      Queue<Buffer> unpackedBuffers = null;
      try {
        unpackedBuffers = BufferPacker.unpack(byteBuf);
        while (!unpackedBuffers.isEmpty()) {
          onBuffer(unpackedBuffers.poll(), channelIdx);
        }
      } catch (Throwable throwable) {
        synchronized (lock) {
          cause = cause == null ? throwable : cause;
          availabilityHelper.getUnavailableToResetAvailable().complete(null);
        }

        if (unpackedBuffers != null) {
          unpackedBuffers.forEach(Buffer::recycleBuffer);
        }
        LOG.error("Failed to process the received buffer.", throwable);
      }
    };
  }

  private Consumer<Throwable> getFailureListener(ResultPartitionID rpID) {
    return throwable -> {
      synchronized (lock) {
        if (cause != null) {
          return;
        }
        Class<?> clazz = PartitionNotFoundException.class;
        if (throwable.getMessage() != null && throwable.getMessage().contains(clazz.getName())) {
          cause = new PartitionNotFoundException(rpID);
        } else {
          cause = throwable;
        }
        availabilityHelper.getUnavailableToResetAvailable().complete(null);
      }
    };
  }

  private Pair<Buffer, InputChannelInfo> getReceived() throws IOException {
    synchronized (lock) {
      healthCheck();
      if (!receivedBuffers.isEmpty()) {
        return receivedBuffers.poll();
      } else {
        if (!allReadersEOF()) {
          availabilityHelper.resetUnavailable();
        }
        return null;
      }
    }
  }

  private void healthCheck() throws IOException {
    if (closed) {
      throw new IOException("Input gate already closed.");
    }
    if (cause != null) {
      if (cause instanceof IOException) {
        throw (IOException) cause;
      } else {
        throw new IOException(cause);
      }
    }
  }

  private boolean allReadersEOF() {
    return numUnconsumedSubpartitions <= 0;
  }

  private Optional<BufferOrEvent> transformBuffer(Buffer buf, InputChannelInfo info)
      throws IOException {
    return Optional.of(
        new BufferOrEvent(decompressBufferIfNeeded(buf), info, !isFinished(), false));
  }

  private Optional<BufferOrEvent> transformEvent(Buffer buffer, InputChannelInfo channelInfo)
      throws IOException {
    final AbstractEvent event;
    try {
      event = EventSerializer.fromBuffer(buffer, getClass().getClassLoader());
    } catch (Throwable t) {
      throw new IOException("Deserialize failure.", t);
    } finally {
      buffer.recycleBuffer();
    }
    if (event.getClass() == EndOfPartitionEvent.class) {
      checkState(
          numSubPartitionsHasNotConsumed[channelInfo.getInputChannelIdx()] > 0,
          "BUG -- EndOfPartitionEvent received repeatedly.");
      numSubPartitionsHasNotConsumed[channelInfo.getInputChannelIdx()]--;
      numUnconsumedSubpartitions--;
      // not the real end.
      if (numSubPartitionsHasNotConsumed[channelInfo.getInputChannelIdx()] != 0) {
        return Optional.empty();
      } else {
        // the real end.
        bufferReaders.get(clientIndexMap[channelInfo.getInputChannelIdx()]).close();
        //         tryOpenSomeChannels();
        if (allReadersEOF()) {
          availabilityHelper.getUnavailableToResetAvailable().complete(null);
        }
      }
    } else if (event.getClass() == EndOfData.class) {
      checkState(!hasReceivedEndOfData(), "ERROR ");
      --pendingEndOfDataEvents;
    }

    return Optional.of(
        new BufferOrEvent(
            event,
            buffer.getDataType().hasPriority(),
            channelInfo,
            !isFinished(),
            buffer.getSize(),
            false));
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
  public int getBuffersInUseCount() {
    return 0;
  }

  @Override
  public void announceBufferSize(int i) {}

  @Override
  public List<InputChannelInfo> getUnfinishedChannels() {
    return Collections.emptyList();
  }

  @Override
  public boolean hasReceivedEndOfData() {
    return pendingEndOfDataEvents <= 0;
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
        taskName, gateIndex, gateDescriptor.toString());
  }

  /** Accommodation for the incompleteness of Flink pluggable shuffle service. */
  private class FakedRemoteInputChannel extends RemoteInputChannel {
    FakedRemoteInputChannel(int channelIndex) {
      super(
          new SingleInputGate(
              "",
              gateIndex,
              new IntermediateDataSetID(),
              ResultPartitionType.BLOCKING,
              0,
              1,
              (a, b, c) -> {},
              () -> null,
              null,
              new FakedMemorySegmentProvider(),
              0),
          channelIndex,
          new ResultPartitionID(),
          new ConnectionID(new InetSocketAddress("", 0), 0),
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
