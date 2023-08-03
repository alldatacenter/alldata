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

import static org.apache.celeborn.plugin.flink.utils.Utils.checkState;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.runtime.checkpoint.channel.InputChannelInfo;
import org.apache.flink.runtime.deployment.InputGateDeploymentDescriptor;
import org.apache.flink.runtime.event.AbstractEvent;
import org.apache.flink.runtime.io.AvailabilityProvider;
import org.apache.flink.runtime.io.network.api.EndOfData;
import org.apache.flink.runtime.io.network.api.EndOfPartitionEvent;
import org.apache.flink.runtime.io.network.api.serialization.EventSerializer;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.BufferDecompressor;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.partition.PartitionNotFoundException;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.consumer.BufferOrEvent;
import org.apache.flink.runtime.shuffle.ShuffleDescriptor;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.function.SupplierWithException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.exception.DriverChangedException;
import org.apache.celeborn.common.exception.PartitionUnRetryAbleException;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.plugin.flink.buffer.BufferPacker;
import org.apache.celeborn.plugin.flink.buffer.TransferBufferPool;
import org.apache.celeborn.plugin.flink.readclient.FlinkShuffleClientImpl;
import org.apache.celeborn.plugin.flink.utils.BufferUtils;

public class RemoteShuffleInputGateDelegation {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteShuffleInputGateDelegation.class);
  /** Lock to protect {@link #receivedBuffers} and {@link #cause} and {@link #closed}. */
  private Object lock = new Object();

  /** Name of the corresponding computing task. */
  private String taskName;

  /** Index of the gate of the corresponding computing task. */
  private int gateIndex;

  /** Deployment descriptor for a single input gate instance. */
  private InputGateDeploymentDescriptor gateDescriptor;

  /** Buffer pool provider. */
  private SupplierWithException<BufferPool, IOException> bufferPoolFactory;

  /** Flink buffer pools to allocate network memory. */
  private BufferPool bufferPool;

  /** Buffer pool used by the transfer layer. */
  private TransferBufferPool transferBufferPool = new TransferBufferPool(Collections.emptySet());

  private List<RemoteBufferStreamReader> bufferReaders = new ArrayList<>();

  private List<InputChannelInfo> channelsInfo;
  /** Map from channel index to reader index. */
  private int[] channelIndexToReaderIndex;

  /** Map from read index to channel index. */
  private int[] readerIndexToChannelIndex;

  /** The number of subpartitions that has not consumed per channel. */
  private int[] numSubPartitionsNotConsumed;

  /** The overall number of subpartitions that has not been consumed. */
  private long numUnconsumedSubpartitions;

  /** Received buffers from remote shuffle worker. It's consumed by upper computing task. */
  private Queue<Pair<Buffer, InputChannelInfo>> receivedBuffers = new LinkedList<>();

  /** {@link Throwable} when reading failure. */
  private Throwable cause;

  /** Whether this remote input gate has been closed or not. */
  private boolean closed;

  /** Whether we have opened all initial channels or not. */
  private boolean initialChannelsOpened;

  /** Number of pending {@link EndOfData} events to be received. */
  private long pendingEndOfDataEvents;

  /** Max concurrent reader count */
  private int numConcurrentReading;

  /** Keep compatibility with streaming mode. */
  private boolean shouldDrainOnEndOfData = true;

  /** Data decompressor. */
  private BufferDecompressor bufferDecompressor;

  private FlinkShuffleClientImpl shuffleClient;

  private int numOpenedReaders = 0;
  private AvailabilityProvider.AvailabilityHelper availabilityHelper;
  private int startSubIndex;
  private int endSubIndex;

  public RemoteShuffleInputGateDelegation(
      CelebornConf celebornConf,
      String taskName,
      int gateIndex,
      InputGateDeploymentDescriptor gateDescriptor,
      SupplierWithException<BufferPool, IOException> bufferPoolFactory,
      BufferDecompressor bufferDecompressor,
      int numConcurrentReading,
      AvailabilityProvider.AvailabilityHelper availabilityHelper,
      int startSubIndex,
      int endSubIndex) {
    this.taskName = taskName;
    this.gateIndex = gateIndex;
    this.gateDescriptor = gateDescriptor;
    this.bufferPoolFactory = bufferPoolFactory;

    int numChannels = gateDescriptor.getShuffleDescriptors().length;
    this.channelIndexToReaderIndex = new int[numChannels];
    this.readerIndexToChannelIndex = new int[numChannels];
    this.numSubPartitionsNotConsumed = new int[numChannels];
    this.bufferDecompressor = bufferDecompressor;

    RemoteShuffleDescriptor remoteShuffleDescriptor =
        (RemoteShuffleDescriptor) gateDescriptor.getShuffleDescriptors()[0];
    RemoteShuffleResource shuffleResource = remoteShuffleDescriptor.getShuffleResource();

    try {
      String appUniqueId =
          ((RemoteShuffleDescriptor) (gateDescriptor.getShuffleDescriptors()[0]))
              .getCelebornAppId();
      this.shuffleClient =
          FlinkShuffleClientImpl.get(
              appUniqueId,
              shuffleResource.getLifecycleManagerHost(),
              shuffleResource.getLifecycleManagerPort(),
              shuffleResource.getLifecycleManagerTimestamp(),
              celebornConf,
              new UserIdentifier("default", "default"));
    } catch (DriverChangedException e) {
      throw new RuntimeException(e.getMessage());
    }

    this.startSubIndex = startSubIndex;
    this.endSubIndex = endSubIndex;

    initShuffleReadClients();

    channelsInfo = createChannelInfos();
    this.numConcurrentReading = numConcurrentReading;
    this.availabilityHelper = availabilityHelper;
    LOG.debug("Initial input gate with numConcurrentReading {}", this.numConcurrentReading);
  }

  private void initShuffleReadClients() {
    int numSubpartitionsPerChannel = endSubIndex - startSubIndex + 1;
    long numUnconsumedSubpartitions = 0;

    // left element is index
    List<Pair<Integer, ShuffleDescriptor>> descriptors =
        IntStream.range(0, gateDescriptor.getShuffleDescriptors().length)
            .mapToObj(i -> Pair.of(i, gateDescriptor.getShuffleDescriptors()[i]))
            .collect(Collectors.toList());

    int readerIndex = 0;
    for (Pair<Integer, ShuffleDescriptor> descriptor : descriptors) {
      RemoteShuffleDescriptor remoteDescriptor = (RemoteShuffleDescriptor) descriptor.getRight();
      ShuffleResourceDescriptor shuffleDescriptor =
          remoteDescriptor.getShuffleResource().getMapPartitionShuffleDescriptor();

      LOG.debug("create shuffle reader for descriptor {}", shuffleDescriptor);

      RemoteBufferStreamReader reader =
          new RemoteBufferStreamReader(
              shuffleClient,
              shuffleDescriptor,
              startSubIndex,
              endSubIndex,
              transferBufferPool,
              getDataListener(descriptor.getLeft()),
              getFailureListener(remoteDescriptor.getResultPartitionID()));

      bufferReaders.add(reader);
      numSubPartitionsNotConsumed[descriptor.getLeft()] = numSubpartitionsPerChannel;
      numUnconsumedSubpartitions += numSubpartitionsPerChannel;
      channelIndexToReaderIndex[descriptor.getLeft()] = readerIndex;
      readerIndexToChannelIndex[readerIndex] = descriptor.getLeft();
      ++readerIndex;
    }

    this.numUnconsumedSubpartitions = numUnconsumedSubpartitions;
    this.pendingEndOfDataEvents = numUnconsumedSubpartitions;
  }

  private List<InputChannelInfo> createChannelInfos() {
    return IntStream.range(0, gateDescriptor.getShuffleDescriptors().length)
        .mapToObj(i -> new InputChannelInfo(gateIndex, i))
        .collect(Collectors.toList());
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
        Class<?> clazz = PartitionUnRetryAbleException.class;
        if (throwable.getMessage() != null && throwable.getMessage().contains(clazz.getName())) {
          cause = new PartitionNotFoundException(rpID);
        } else {
          cause = throwable;
        }
        availabilityHelper.getUnavailableToResetAvailable().complete(null);
      }
    };
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

  public void setup() throws IOException {
    long startTime = System.nanoTime();

    bufferPool = bufferPoolFactory.get();
    BufferUtils.reserveNumRequiredBuffers(bufferPool, 16);
    tryRequestBuffers();
    availabilityHelper.getUnavailableToResetAvailable().complete(null);
    // Complete availability future though handshake not fired yet, thus to allow fetcher to
    // 'pollNext' and fire handshake to remote. This mechanism is to avoid bookkeeping remote
    // reading resource before task start processing data from input gate.

    LOG.info("Set up read gate by {} ms.", (System.nanoTime() - startTime) / 1000_000);
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

  public List<InputChannelInfo> getChannelsInfo() {
    return channelsInfo;
  }

  /** Each one corresponds to a reading channel. */
  public List<RemoteBufferStreamReader> getBufferReaders() {
    return bufferReaders;
  }

  /** Try to open more readers to {@link #numConcurrentReading}. */
  private void tryOpenSomeChannels() throws IOException {
    if (bufferReaders.size() == numOpenedReaders) {
      // all readers are already opened
      return;
    }

    List<RemoteBufferStreamReader> readersToOpen = new ArrayList<>();

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
            numSubPartitionsNotConsumed[readerIndexToChannelIndex[i]]);
        if (numOnGoing >= numConcurrentReading) {
          break;
        }

        if (bufferStreamReader.isOpened()
            && numSubPartitionsNotConsumed[readerIndexToChannelIndex[i]] > 0) {
          numOnGoing++;
          continue;
        }

        if (!bufferStreamReader.isOpened()) {
          readersToOpen.add(bufferStreamReader);
          numOnGoing++;
        }
      }
    }

    for (RemoteBufferStreamReader reader : readersToOpen) {
      reader.open(0);
      numOpenedReaders++;
    }
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
          numSubPartitionsNotConsumed[channelInfo.getInputChannelIdx()] > 0,
          "BUG -- EndOfPartitionEvent received repeatedly.");
      numSubPartitionsNotConsumed[channelInfo.getInputChannelIdx()]--;
      numUnconsumedSubpartitions--;
      // not the real end.
      if (numSubPartitionsNotConsumed[channelInfo.getInputChannelIdx()] != 0) {
        LOG.debug(
            "numSubPartitionsNotConsumed: {}",
            numSubPartitionsNotConsumed[channelInfo.getInputChannelIdx()]);
      } else {
        // the real end.
        bufferReaders.get(channelIndexToReaderIndex[channelInfo.getInputChannelIdx()]).close();
        tryOpenSomeChannels();
        if (allReadersEOF()) {
          availabilityHelper.getUnavailableToResetAvailable().complete(null);
        }
      }
    } else if (event.getClass() == EndOfData.class) {
      checkState(!hasReceivedEndOfData(), "Too many EndOfData event.");
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

  public boolean isFinished() {
    synchronized (lock) {
      return allReadersEOF() && receivedBuffers.isEmpty();
    }
  }

  public boolean hasReceivedEndOfData() {
    return pendingEndOfDataEvents <= 0;
  }

  public int getGateIndex() {
    return gateIndex;
  }

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
      if (buffer.isBuffer()) {
        bufferOrEvent = transformBuffer(buffer, channelInfo);
      } else {
        bufferOrEvent = transformEvent(buffer, channelInfo);
        LOG.debug(
            "received event: {}.",
            bufferOrEvent.isPresent()
                ? bufferOrEvent.get().getEvent().getClass().getName()
                : Optional.empty());
      }

      if (bufferOrEvent.isPresent()) {
        break;
      }
      pair = getReceived();
    }

    tryRequestBuffers();
    return bufferOrEvent;
  }

  public void close() throws Exception {
    List<Buffer> buffersToRecycle;
    Throwable closeException = null;
    // Do not check closed flag, thus to allow calling this method from both task thread and
    // cancel thread.
    for (RemoteBufferStreamReader reader : bufferReaders) {
      try {
        reader.close();
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

  public String getTaskName() {
    return taskName;
  }

  public InputGateDeploymentDescriptor getGateDescriptor() {
    return gateDescriptor;
  }

  public long getPendingEndOfDataEvents() {
    return pendingEndOfDataEvents;
  }
}
