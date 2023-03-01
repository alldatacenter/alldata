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

package org.apache.celeborn.common.network.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.annotation.concurrent.GuardedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.meta.FileInfo;
import org.apache.celeborn.common.network.server.memory.BufferRecycler;
import org.apache.celeborn.common.network.server.memory.MemoryManager;

public class BufferStreamManager {
  private static final Logger logger = LoggerFactory.getLogger(BufferStreamManager.class);
  private final AtomicLong nextStreamId;
  protected final ConcurrentHashMap<Long, StreamState> streams;
  protected final ConcurrentHashMap<Long, MapDataPartition> servingStreams;
  protected final ConcurrentHashMap<FileInfo, MapDataPartition> activeMapPartitions;
  protected final MemoryManager memoryManager = MemoryManager.instance();
  protected final StorageFetcherPool storageFetcherPool = new StorageFetcherPool();
  protected int minReadBuffers;
  protected int maxReadBuffers;
  protected int threadsPerMountPoint;
  private final BlockingQueue<DelayedStreamId> recycleStreamIds = new DelayQueue<>();

  @GuardedBy("lock")
  private volatile Thread recycleThread;

  private final Object lock = new Object();

  protected class StreamState {
    private Channel associatedChannel;
    private int bufferSize;

    public StreamState(Channel associatedChannel, int bufferSize) {
      this.associatedChannel = associatedChannel;
      this.bufferSize = bufferSize;
    }

    public Channel getAssociatedChannel() {
      return associatedChannel;
    }

    public int getBufferSize() {
      return bufferSize;
    }
  }

  public BufferStreamManager(int minReadBuffers, int maxReadBuffers, int threadsPerMountpoint) {
    nextStreamId = new AtomicLong((long) new Random().nextInt(Integer.MAX_VALUE) * 1000);
    streams = new ConcurrentHashMap<>();
    servingStreams = new ConcurrentHashMap<>();
    activeMapPartitions = new ConcurrentHashMap<>();
    this.minReadBuffers = minReadBuffers;
    this.maxReadBuffers = maxReadBuffers;
    this.threadsPerMountPoint = threadsPerMountpoint;
  }

  public long registerStream(
      Consumer<Long> callback,
      Channel channel,
      int initialCredit,
      int startSubIndex,
      int endSubIndex,
      FileInfo fileInfo)
      throws IOException {
    long streamId = nextStreamId.getAndIncrement();
    streams.put(streamId, new StreamState(channel, fileInfo.getBufferSize()));
    logger.debug("Register stream start streamId: {}, fileInfo: {}", streamId, fileInfo);
    synchronized (activeMapPartitions) {
      MapDataPartition mapDataPartition = activeMapPartitions.get(fileInfo);
      if (mapDataPartition == null) {
        mapDataPartition = new MapDataPartition(fileInfo);
        activeMapPartitions.put(fileInfo, mapDataPartition);
      }
      mapDataPartition.addStream(streamId);
      addCredit(initialCredit, streamId);
      servingStreams.put(streamId, mapDataPartition);
      // response streamId to channel first
      callback.accept(streamId);
      mapDataPartition.setupDataPartitionReader(startSubIndex, endSubIndex, streamId, channel);
    }

    logger.debug("Register stream streamId: {}, fileInfo: {}", streamId, fileInfo);

    return streamId;
  }

  public void addCredit(int numCredit, long streamId) {
    logger.debug("streamId: {}, add credit: {}", streamId, numCredit);
    try {
      MapDataPartition mapDataPartition = servingStreams.get(streamId);
      if (mapDataPartition != null) {
        mapDataPartition.addReaderCredit(numCredit, streamId);
      }
    } catch (Throwable e) {
      logger.error("streamId: {}, add credit end: {}", streamId, numCredit);
    }
  }

  public void connectionTerminated(Channel channel) {
    for (Map.Entry<Long, StreamState> entry : streams.entrySet()) {
      if (entry.getValue().getAssociatedChannel() == channel) {
        logger.info("connection closed, clean streamId: {}", entry.getKey());
        recycleStream(entry.getKey());
      }
    }
  }

  public void recycleStream(long streamId) {
    recycleStreamIds.add(new DelayedStreamId(streamId));
    startRecycleThread(); // lazy start thread
  }

  private void startRecycleThread() {
    if (recycleThread == null) {
      synchronized (lock) {
        if (recycleThread == null) {
          recycleThread =
              new Thread(
                  () -> {
                    while (true) {
                      try {
                        DelayedStreamId delayedStreamId = recycleStreamIds.take();
                        cleanResource(delayedStreamId.streamId);
                      } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                      }
                    }
                  },
                  "recycle-thread");
          recycleThread.setDaemon(true);
          recycleThread.start();

          logger.info("start stream recycle thread");
        }
      }
    }
  }

  public void cleanResource(Long streamId) {
    logger.debug("received clean stream: {}", streamId);
    if (streams.containsKey(streamId)) {
      MapDataPartition mapDataPartition = servingStreams.get(streamId);
      if (mapDataPartition != null) {
        if (mapDataPartition.releaseStream(streamId)) {
          synchronized (activeMapPartitions) {
            if (mapDataPartition.activeStreamIds.isEmpty()) {
              mapDataPartition.close();
              FileInfo fileInfo = mapDataPartition.fileInfo;
              activeMapPartitions.remove(fileInfo);
            }
          }
        } else {
          logger.debug("retry clean stream: {}", streamId);
          recycleStreamIds.add(new DelayedStreamId(streamId));
        }
      }
    }
  }

  public static class DelayedStreamId implements Delayed {
    private static final long delayTime = 100; // 100ms
    private long createMillis = System.currentTimeMillis();

    private long streamId;

    public DelayedStreamId(long streamId) {
      this.createMillis = createMillis + delayTime;
      this.streamId = streamId;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      long diff = createMillis - System.currentTimeMillis();
      return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    public long getCreateMillis() {
      return createMillis;
    }

    @Override
    public int compareTo(Delayed o) {
      long otherCreateMillis = ((DelayedStreamId) o).getCreateMillis();
      if (this.createMillis < otherCreateMillis) {
        return -1;
      } else if (this.createMillis > otherCreateMillis) {
        return 1;
      }

      return 0;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("DelayedStreamId{");
      sb.append("createMillis=").append(createMillis);
      sb.append(", streamId=").append(streamId);
      sb.append('}');
      return sb.toString();
    }
  }

  // this means active data partition
  protected class MapDataPartition {
    private final List<Long> activeStreamIds = new ArrayList<>();
    private final FileInfo fileInfo;
    private final Set<DataPartitionReader> readers = new HashSet<>();
    private final ExecutorService readExecutor;
    private final ConcurrentHashMap<Long, DataPartitionReader> streamReaders =
        new ConcurrentHashMap<>();

    /** All available buffers can be used by the partition readers for reading. */
    private Queue<ByteBuf> buffers;

    private FileChannel dataFileChanel;
    private FileChannel indexChannel;

    private boolean isReleased;

    public MapDataPartition(FileInfo fileInfo) throws FileNotFoundException {
      this.fileInfo = fileInfo;
      readExecutor = storageFetcherPool.getExecutorPool(fileInfo.getMountPoint());
      this.dataFileChanel = new FileInputStream(fileInfo.getFile()).getChannel();
      this.indexChannel = new FileInputStream(fileInfo.getIndexPath()).getChannel();
    }

    public synchronized void setupDataPartitionReader(
        int startSubIndex, int endSubIndex, long streamId, Channel channel) throws IOException {
      DataPartitionReader dataPartitionReader =
          new DataPartitionReader(
              startSubIndex,
              endSubIndex,
              fileInfo,
              streamId,
              channel,
              () -> recycleStream(streamId));
      dataPartitionReader.open(dataFileChanel, indexChannel);
      // allocate resources when the first reader is registered
      boolean allocateResources = readers.isEmpty();
      readers.add(dataPartitionReader);
      streamReaders.put(streamId, dataPartitionReader);

      // create initial buffers for read
      if (allocateResources && buffers == null) {
        memoryManager.requestReadBuffers(
            minReadBuffers,
            maxReadBuffers,
            fileInfo.getBufferSize(),
            (allocatedBuffers, throwable) ->
                MapDataPartition.this.onBuffer(new LinkedBlockingDeque<>(allocatedBuffers)));
      } else {
        triggerRead();
      }
    }

    // Read logic is executed on another thread.
    public void onBuffer(Queue<ByteBuf> buffers) {
      this.buffers = buffers;
      triggerRead();
    }

    public void recycle(ByteBuf buffer, Queue<ByteBuf> bufferQueue) {
      buffer.clear();
      bufferQueue.add(buffer);
      triggerRead();
    }

    public synchronized void readBuffers() {
      if (isReleased) {
        // some read executor task may already be submitted to the threadpool
        return;
      }

      try {
        PriorityQueue<DataPartitionReader> sortedReaders = new PriorityQueue<>(readers);
        while (buffers != null && buffers.size() > 0 && !sortedReaders.isEmpty()) {
          BufferRecycler bufferRecycler =
              new BufferRecycler(memoryManager, (buffer) -> this.recycle(buffer, buffers));
          DataPartitionReader reader = sortedReaders.poll();
          try {
            if (!reader.readAndSend(buffers, bufferRecycler)) {
              readers.remove(reader);
            }
          } catch (Throwable e) {
            logger.error("reader exception, reader: {}, message: {}", reader, e.getMessage(), e);
            readers.remove(reader);
            reader.recycleOnError(e);
          }
        }
      } catch (Throwable e) {
        logger.error("Fatal: failed to read partition data. {}", e.getMessage(), e);
        for (DataPartitionReader reader : readers) {
          reader.recycleOnError(e);
        }

        readers.clear();
      }
    }

    // for one reader only the associated channel can access
    public void addReaderCredit(int numCredit, long streamId) {
      DataPartitionReader streamReader = this.getStreamReader(streamId);
      if (streamReader != null) {
        boolean canSendWithCredit = streamReader.sendWithCredit(numCredit);
        if (canSendWithCredit) {
          readExecutor.submit(() -> streamReader.sendData());
        }
      }
    }

    public void triggerRead() {
      // Key for IO schedule.
      readExecutor.submit(() -> readBuffers());
    }

    public void addStream(Long streamId) {
      synchronized (activeStreamIds) {
        activeStreamIds.add(streamId);
      }
    }

    public void removeStream(Long streamId) {
      synchronized (activeStreamIds) {
        activeStreamIds.remove(streamId);
        streamReaders.remove(streamId);
      }
    }

    public DataPartitionReader getStreamReader(long streamId) {
      return streamReaders.get(streamId);
    }

    public boolean releaseStream(Long streamId) {
      DataPartitionReader dataPartitionReader = streamReaders.get(streamId);
      dataPartitionReader.release();
      if (dataPartitionReader.isFinished()) {
        logger.info("release all for stream: {}", streamId);
        removeStream(streamId);
        streams.remove(streamId);
        servingStreams.remove(streamId);
        return true;
      }

      return false;
    }

    public void close() {
      logger.info("release map data partition {}", fileInfo);

      IOUtils.closeQuietly(dataFileChanel);
      IOUtils.closeQuietly(indexChannel);

      if (this.buffers != null) {
        for (ByteBuf buffer : this.buffers) {
          memoryManager.recycleReadBuffer(buffer);
        }
      }

      this.buffers = null;

      isReleased = true;
    }
  }

  class StorageFetcherPool {
    private final HashMap<String, ExecutorService> executorPools = new HashMap<>();

    public ExecutorService getExecutorPool(String mountPoint) {
      // it's ok if the mountpoint is unknown
      return executorPools.computeIfAbsent(
          mountPoint,
          k ->
              Executors.newFixedThreadPool(
                  threadsPerMountPoint,
                  new ThreadFactoryBuilder()
                      .setNameFormat("reader-thread-%d")
                      .setUncaughtExceptionHandler(
                          (t1, t2) -> {
                            logger.warn("StorageFetcherPool thread:{}:{}", t1, t2);
                          })
                      .build()));
    }
  }
}
