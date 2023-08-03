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

package org.apache.celeborn.service.deploy.worker.storage;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.meta.FileInfo;
import org.apache.celeborn.common.util.JavaUtils;
import org.apache.celeborn.service.deploy.worker.memory.BufferQueue;
import org.apache.celeborn.service.deploy.worker.memory.BufferRecycler;
import org.apache.celeborn.service.deploy.worker.memory.MemoryManager;

// this means active data partition
class MapDataPartition implements MemoryManager.ReadBufferTargetChangeListener {
  public static final Logger logger = LoggerFactory.getLogger(MapDataPartition.class);
  private final FileInfo fileInfo;
  private final ExecutorService readExecutor;
  private final ConcurrentHashMap<Long, MapDataPartitionReader> readers =
      JavaUtils.newConcurrentHashMap();
  private FileChannel dataFileChanel;
  private FileChannel indexChannel;
  private long indexSize;
  private volatile boolean isReleased = false;
  private final BufferQueue bufferQueue = new BufferQueue();
  private AtomicBoolean bufferQueueInitialized = new AtomicBoolean(false);
  private MemoryManager memoryManager = MemoryManager.instance();
  private Consumer<Long> recycleStream;
  private int minReadBuffers;
  private int maxReadBuffers;
  private int minBuffersToTriggerRead;
  private AtomicBoolean hasReadingTask = new AtomicBoolean(false);

  public MapDataPartition(
      int minReadBuffers,
      int maxReadBuffers,
      HashMap<String, ExecutorService> storageFetcherPool,
      int threadsPerMountPoint,
      FileInfo fileInfo,
      Consumer<Long> recycleStream,
      int minBuffersToTriggerRead)
      throws IOException {
    this.recycleStream = recycleStream;
    this.fileInfo = fileInfo;

    this.minReadBuffers = minReadBuffers;
    this.maxReadBuffers = maxReadBuffers;

    updateBuffersTarget((this.minReadBuffers + this.maxReadBuffers) / 2 + 1);

    logger.debug(
        "read map partition {} with {} {} {}",
        fileInfo.getFilePath(),
        bufferQueue.getLocalBuffersTarget(),
        fileInfo.getBufferSize());

    this.minBuffersToTriggerRead = minBuffersToTriggerRead;

    readExecutor =
        storageFetcherPool.computeIfAbsent(
            fileInfo.getMountPoint(),
            k ->
                Executors.newFixedThreadPool(
                    threadsPerMountPoint,
                    new ThreadFactoryBuilder()
                        .setNameFormat(fileInfo.getMountPoint() + "-reader-thread-%d")
                        .setUncaughtExceptionHandler(
                            (t1, t2) -> {
                              logger.warn("StorageFetcherPool thread:{}:{}", t1, t2);
                            })
                        .build()));
    this.dataFileChanel = FileChannelUtils.openReadableFileChannel(fileInfo.getFilePath());
    this.indexChannel = FileChannelUtils.openReadableFileChannel(fileInfo.getIndexPath());
    this.indexSize = indexChannel.size();

    MemoryManager.instance().addReadBufferTargetChangeListener(this);
  }

  private synchronized void updateBuffersTarget(int buffersTarget) {
    int currentBuffersTarget = buffersTarget;
    if (currentBuffersTarget < minReadBuffers) {
      currentBuffersTarget = minReadBuffers;
    }
    if (currentBuffersTarget > maxReadBuffers) {
      currentBuffersTarget = maxReadBuffers;
    }
    bufferQueue.setLocalBuffersTarget(currentBuffersTarget);
  }

  public void setupDataPartitionReader(
      int startSubIndex, int endSubIndex, long streamId, Channel channel) {
    MapDataPartitionReader mapDataPartitionReader =
        new MapDataPartitionReader(
            startSubIndex,
            endSubIndex,
            fileInfo,
            streamId,
            channel,
            () -> recycleStream.accept(streamId));
    readers.put(streamId, mapDataPartitionReader);
  }

  public void tryRequestBufferOrRead() {
    if (bufferQueueInitialized.compareAndSet(false, true)) {
      bufferQueue.tryApplyNewBuffers(
          readers.size(),
          fileInfo.getBufferSize(),
          (allocatedBuffers, throwable) -> onBuffer(allocatedBuffers));
    } else {
      triggerRead();
    }
  }

  // Read logic is executed on another thread.
  public void onBuffer(List<ByteBuf> buffers) {
    if (isReleased) {
      buffers.forEach(memoryManager::recycleReadBuffer);
      return;
    }

    bufferQueue.add(buffers);

    if (bufferQueue.size()
        >= Math.min(bufferQueue.getLocalBuffersTarget() / 2 + 1, minBuffersToTriggerRead)) {
      triggerRead();
    }
  }

  public void recycle(ByteBuf buffer) {
    if (isReleased) {
      // this means bufferQueue is already release
      memoryManager.recycleReadBuffer(buffer);
      return;
    }

    bufferQueue.recycle(buffer);

    if (bufferQueue.size()
        >= Math.min(bufferQueue.getLocalBuffersTarget() / 2 + 1, minBuffersToTriggerRead)) {
      triggerRead();
    }

    bufferQueue.tryApplyNewBuffers(
        readers.size(),
        fileInfo.getBufferSize(),
        (allocatedBuffers, throwable) -> onBuffer(allocatedBuffers));
  }

  public synchronized void readBuffers() {
    hasReadingTask.set(false);
    if (isReleased) {
      // some read executor task may already be submitted to the thread pool
      return;
    }

    try {
      PriorityQueue<MapDataPartitionReader> sortedReaders =
          new PriorityQueue<>(
              readers.values().stream()
                  .filter(MapDataPartitionReader::shouldReadData)
                  .collect(Collectors.toList()));
      for (MapDataPartitionReader reader : sortedReaders) {
        reader.open(dataFileChanel, indexChannel, indexSize);
      }
      while (bufferQueue.bufferAvailable() && !sortedReaders.isEmpty()) {
        BufferRecycler bufferRecycler = new BufferRecycler(MapDataPartition.this::recycle);
        MapDataPartitionReader reader = sortedReaders.poll();
        try {
          reader.readData(bufferQueue, bufferRecycler);
        } catch (Throwable e) {
          logger.error("reader exception, reader: {}, message: {}", reader, e.getMessage(), e);
          reader.recycleOnError(e);
        }
      }
    } catch (Throwable e) {
      logger.error("Fatal: failed to read partition data. {}", e.getMessage(), e);
      for (MapDataPartitionReader reader : readers.values()) {
        reader.recycleOnError(e);
      }
    }
  }

  // for one reader only the associated channel can access
  public void addReaderCredit(int numCredit, long streamId) {
    MapDataPartitionReader streamReader = getStreamReader(streamId);
    if (streamReader != null) {
      streamReader.addCredit(numCredit);
      readExecutor.submit(() -> streamReader.sendData());
    }
  }

  public void triggerRead() {
    // Key for IO schedule.
    if (hasReadingTask.compareAndSet(false, true)) {
      readExecutor.submit(() -> readBuffers());
    }
  }

  public MapDataPartitionReader getStreamReader(long streamId) {
    return readers.get(streamId);
  }

  public boolean releaseReader(Long streamId) {
    MapDataPartitionReader mapDataPartitionReader = readers.get(streamId);
    mapDataPartitionReader.release();
    if (mapDataPartitionReader.isFinished()) {
      logger.debug("release all for stream: {}", streamId);
      readers.remove(streamId);
      return true;
    }

    return false;
  }

  public void close() {
    logger.debug("release map data partition {}", fileInfo);
    bufferQueue.release();
    isReleased = true;

    IOUtils.closeQuietly(dataFileChanel);
    IOUtils.closeQuietly(indexChannel);

    MemoryManager.instance().removeReadBufferTargetChangeListener(this);
  }

  @Override
  public String toString() {
    return "MapDataPartition{" + "fileInfo=" + fileInfo.getFilePath() + '}';
  }

  public ConcurrentHashMap<Long, MapDataPartitionReader> getReaders() {
    return readers;
  }

  public FileInfo getFileInfo() {
    return fileInfo;
  }

  @Override
  public void onChange(long newMemoryTarget) {
    updateBuffersTarget((int) Math.ceil(newMemoryTarget * 1.0 / fileInfo.getBufferSize()));
    bufferQueue.trim();
  }
}
