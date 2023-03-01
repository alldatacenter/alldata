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

package org.apache.uniffle.server.buffer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeRangeMap;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShufflePartitionedData;
import org.apache.uniffle.common.rpc.StatusCode;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.common.util.TripleFunction;
import org.apache.uniffle.server.ShuffleDataFlushEvent;
import org.apache.uniffle.server.ShuffleFlushManager;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.server.ShuffleServerMetrics;

public class ShuffleBufferManager {

  private static final Logger LOG = LoggerFactory.getLogger(ShuffleBufferManager.class);

  private final ShuffleFlushManager shuffleFlushManager;
  private long capacity;
  private long readCapacity;
  private int retryNum;
  private long highWaterMark;
  private long lowWaterMark;
  private boolean bufferFlushEnabled;
  private long bufferFlushThreshold;
  // when shuffle buffer manager flushes data, shuffles with data size < shuffleFlushThreshold is kept in memory to
  // reduce small I/Os to persistent storage, especially for local HDDs.
  private long shuffleFlushThreshold;
  // Huge partition vars
  private long hugePartitionSizeThreshold;
  private long hugePartitionMemoryLimitSize;

  protected long bufferSize = 0;
  protected AtomicLong preAllocatedSize = new AtomicLong(0L);
  protected AtomicLong inFlushSize = new AtomicLong(0L);
  protected AtomicLong usedMemory = new AtomicLong(0L);
  private AtomicLong readDataMemory = new AtomicLong(0L);
  // appId -> shuffleId -> partitionId -> ShuffleBuffer to avoid too many appId
  protected Map<String, Map<Integer, RangeMap<Integer, ShuffleBuffer>>> bufferPool;
  // appId -> shuffleId -> shuffle size in buffer
  protected Map<String, Map<Integer, AtomicLong>> shuffleSizeMap = Maps.newConcurrentMap();

  public ShuffleBufferManager(ShuffleServerConf conf, ShuffleFlushManager shuffleFlushManager) {
    long heapSize = Runtime.getRuntime().maxMemory();
    this.capacity = conf.getSizeAsBytes(ShuffleServerConf.SERVER_BUFFER_CAPACITY);
    if (this.capacity < 0) {
      this.capacity = (long) (heapSize * conf.getDouble(ShuffleServerConf.SERVER_BUFFER_CAPACITY_RATIO));
    }
    this.readCapacity = conf.getSizeAsBytes(ShuffleServerConf.SERVER_READ_BUFFER_CAPACITY);
    if (this.readCapacity < 0) {
      this.readCapacity = (long) (heapSize * conf.getDouble(ShuffleServerConf.SERVER_READ_BUFFER_CAPACITY_RATIO));
    }
    LOG.info("Init shuffle buffer manager with capacity: {}, read buffer capacity: {}.", capacity, readCapacity);
    this.shuffleFlushManager = shuffleFlushManager;
    this.bufferPool = new ConcurrentHashMap<>();
    this.retryNum = conf.getInteger(ShuffleServerConf.SERVER_MEMORY_REQUEST_RETRY_MAX);
    this.highWaterMark = (long)(capacity / 100.0
        * conf.get(ShuffleServerConf.SERVER_MEMORY_SHUFFLE_HIGHWATERMARK_PERCENTAGE));
    this.lowWaterMark = (long)(capacity / 100.0
        * conf.get(ShuffleServerConf.SERVER_MEMORY_SHUFFLE_LOWWATERMARK_PERCENTAGE));
    this.bufferFlushEnabled = conf.getBoolean(ShuffleServerConf.SINGLE_BUFFER_FLUSH_ENABLED);
    this.bufferFlushThreshold = conf.getLong(ShuffleServerConf.SINGLE_BUFFER_FLUSH_THRESHOLD);
    this.shuffleFlushThreshold = conf.getLong(ShuffleServerConf.SERVER_SHUFFLE_FLUSH_THRESHOLD);
    this.hugePartitionSizeThreshold = conf.getSizeAsBytes(ShuffleServerConf.HUGE_PARTITION_SIZE_THRESHOLD);
    this.hugePartitionMemoryLimitSize = Math.round(
        capacity * conf.get(ShuffleServerConf.HUGE_PARTITION_MEMORY_USAGE_LIMITATION_RATIO)
    );
  }

  public StatusCode registerBuffer(String appId, int shuffleId, int startPartition, int endPartition) {
    bufferPool.putIfAbsent(appId, Maps.newConcurrentMap());
    Map<Integer, RangeMap<Integer, ShuffleBuffer>> shuffleIdToBuffers = bufferPool.get(appId);
    shuffleIdToBuffers.putIfAbsent(shuffleId, TreeRangeMap.create());
    RangeMap<Integer, ShuffleBuffer> bufferRangeMap = shuffleIdToBuffers.get(shuffleId);
    if (bufferRangeMap.get(startPartition) == null) {
      ShuffleServerMetrics.counterTotalPartitionNum.inc();
      ShuffleServerMetrics.gaugeTotalPartitionNum.inc();
      bufferRangeMap.put(Range.closed(startPartition, endPartition), new ShuffleBuffer(bufferSize));
    } else {
      LOG.warn("Already register for appId[" + appId + "], shuffleId[" + shuffleId + "], startPartition["
              + startPartition + "], endPartition[" + endPartition + "]");
    }

    return StatusCode.SUCCESS;
  }

  // Only for tests
  public StatusCode cacheShuffleData(
      String appId,
      int shuffleId,
      boolean isPreAllocated,
      ShufflePartitionedData spd) {
    return cacheShuffleData(
        appId,
        shuffleId,
        isPreAllocated,
        spd,
        null
    );
  }

  public StatusCode cacheShuffleData(
      String appId,
      int shuffleId,
      boolean isPreAllocated,
      ShufflePartitionedData spd,
      TripleFunction<String, Integer, Integer, Long> getPartitionDataSizeFunc) {
    if (!isPreAllocated && isFull()) {
      LOG.warn("Got unexpected data, can't cache it because the space is full");
      return StatusCode.NO_BUFFER;
    }

    Entry<Range<Integer>, ShuffleBuffer> entry = getShuffleBufferEntry(
        appId, shuffleId, spd.getPartitionId());
    if (entry == null) {
      return StatusCode.NO_REGISTER;
    }

    ShuffleBuffer buffer = entry.getValue();
    long size = buffer.append(spd);
    if (!isPreAllocated) {
      updateUsedMemory(size);
    }
    updateShuffleSize(appId, shuffleId, size);
    synchronized (this) {
      flushSingleBufferIfNecessary(
          buffer,
          appId,
          shuffleId,
          spd.getPartitionId(),
          entry.getKey().lowerEndpoint(),
          entry.getKey().upperEndpoint(),
          getPartitionDataSizeFunc
      );
      flushIfNecessary();
    }
    return StatusCode.SUCCESS;
  }

  private void updateShuffleSize(String appId, int shuffleId, long size) {
    shuffleSizeMap.putIfAbsent(appId, Maps.newConcurrentMap());
    Map<Integer, AtomicLong> shuffleIdToSize = shuffleSizeMap.get(appId);
    shuffleIdToSize.putIfAbsent(shuffleId, new AtomicLong(0));
    shuffleIdToSize.get(shuffleId).addAndGet(size);
  }

  public Entry<Range<Integer>, ShuffleBuffer> getShuffleBufferEntry(
      String appId, int shuffleId, int partitionId) {
    Map<Integer, RangeMap<Integer, ShuffleBuffer>> shuffleIdToBuffers = bufferPool.get(appId);
    if (shuffleIdToBuffers == null) {
      return null;
    }
    RangeMap<Integer, ShuffleBuffer> rangeToBuffers = shuffleIdToBuffers.get(shuffleId);
    if (rangeToBuffers == null) {
      return null;
    }
    Entry<Range<Integer>, ShuffleBuffer> entry = rangeToBuffers.getEntry(partitionId);
    if (entry == null) {
      return null;
    }
    return entry;
  }

  public ShuffleDataResult getShuffleData(
      String appId, int shuffleId, int partitionId, long blockId,
      int readBufferSize) {
    return getShuffleData(
        appId,
        shuffleId,
        partitionId,
        blockId,
        readBufferSize,
        null
    );
  }

  public ShuffleDataResult getShuffleData(
      String appId, int shuffleId, int partitionId, long blockId,
      int readBufferSize, Roaring64NavigableMap expectedTaskIds) {
    Map.Entry<Range<Integer>, ShuffleBuffer> entry = getShuffleBufferEntry(
        appId, shuffleId, partitionId);
    if (entry == null) {
      return null;
    }

    ShuffleBuffer buffer = entry.getValue();
    if (buffer == null) {
      return null;
    }
    return buffer.getShuffleData(blockId, readBufferSize, expectedTaskIds);
  }

  void flushSingleBufferIfNecessary(
      ShuffleBuffer buffer,
      String appId,
      int shuffleId,
      int partitionId,
      int startPartition,
      int endPartition,
      TripleFunction<String, Integer, Integer, Long> getPartitionDataSizeFunc) {
    // When we use multi storage and trigger single buffer flush, the buffer size should be bigger
    // than rss.server.flush.cold.storage.threshold.size, otherwise cold storage will be useless.
    if (this.bufferFlushEnabled && buffer.getSize() > this.bufferFlushThreshold) {
      flushBuffer(buffer, appId, shuffleId, startPartition, endPartition);
      return;
    }

    if (getPartitionDataSizeFunc != null
        && getPartitionDataSizeFunc.accept(appId, shuffleId, partitionId) > hugePartitionSizeThreshold
        && buffer.getSize() > this.bufferFlushThreshold) {
      flushBuffer(buffer, appId, shuffleId, startPartition, endPartition);
      return;
    }
  }

  public void flushIfNecessary() {
    // if data size in buffer > highWaterMark, do the flush
    if (usedMemory.get() - preAllocatedSize.get() - inFlushSize.get() > highWaterMark) {
      // todo: add a metric here to track how many times flush occurs.
      LOG.info("Start to flush with usedMemory[{}], preAllocatedSize[{}], inFlushSize[{}]",
          usedMemory.get(), preAllocatedSize.get(), inFlushSize.get());
      Map<String, Set<Integer>> pickedShuffle = pickFlushedShuffle();
      flush(pickedShuffle);
    }
  }

  public synchronized void commitShuffleTask(String appId, int shuffleId) {
    RangeMap<Integer, ShuffleBuffer> buffers = bufferPool.get(appId).get(shuffleId);
    for (Map.Entry<Range<Integer>, ShuffleBuffer> entry : buffers.asMapOfRanges().entrySet()) {
      ShuffleBuffer buffer = entry.getValue();
      Range<Integer> range = entry.getKey();
      flushBuffer(buffer, appId, shuffleId, range.lowerEndpoint(), range.upperEndpoint());
    }
  }

  protected void flushBuffer(ShuffleBuffer buffer, String appId,
      int shuffleId, int startPartition, int endPartition) {
    ShuffleDataFlushEvent event =
        buffer.toFlushEvent(
            appId,
            shuffleId,
            startPartition,
            endPartition,
            () -> bufferPool.containsKey(appId),
            shuffleFlushManager.getDataDistributionType(appId)
        );
    if (event != null) {
      event.addCleanupCallback(() -> releaseMemory(event.getSize(), true, false));
      updateShuffleSize(appId, shuffleId, -event.getSize());
      inFlushSize.addAndGet(event.getSize());
      ShuffleServerMetrics.gaugeInFlushBufferSize.set(inFlushSize.get());
      shuffleFlushManager.addToFlushQueue(event);
    }
  }

  public void removeBuffer(String appId) {
    Map<Integer, RangeMap<Integer, ShuffleBuffer>> shuffleIdToBuffers = bufferPool.get(appId);
    if (shuffleIdToBuffers == null) {
      return;
    }
    removeBufferByShuffleId(appId, shuffleIdToBuffers.keySet());
    shuffleSizeMap.remove(appId);
    bufferPool.remove(appId);
  }

  public synchronized boolean requireMemory(long size, boolean isPreAllocated) {
    if (capacity - usedMemory.get() >= size) {
      usedMemory.addAndGet(size);
      ShuffleServerMetrics.gaugeUsedBufferSize.set(usedMemory.get());
      if (isPreAllocated) {
        requirePreAllocatedSize(size);
      }
      return true;
    }
    LOG.debug("Require memory failed with " + size + " bytes, usedMemory[" + usedMemory.get()
        + "] include preAllocation[" + preAllocatedSize.get()
        + "], inFlushSize[" + inFlushSize.get() + "]");
    return false;
  }

  public void releaseMemory(long size, boolean isReleaseFlushMemory, boolean isReleasePreAllocation) {
    if (usedMemory.get() >= size) {
      usedMemory.addAndGet(-size);
    } else {
      LOG.warn("Current allocated memory[" + usedMemory.get()
          + "] is less than released[" + size + "], set allocated memory to 0");
      usedMemory.set(0L);
    }

    ShuffleServerMetrics.gaugeUsedBufferSize.set(usedMemory.get());

    if (isReleaseFlushMemory) {
      releaseFlushMemory(size);
    }

    if (isReleasePreAllocation) {
      releasePreAllocatedSize(size);
    }
  }

  private void releaseFlushMemory(long size) {
    if (inFlushSize.get() >= size) {
      inFlushSize.addAndGet(-size);
    } else {
      LOG.warn("Current in flush memory[" + inFlushSize.get()
          + "] is less than released[" + size + "], set allocated memory to 0");
      inFlushSize.set(0L);
    }
    ShuffleServerMetrics.gaugeInFlushBufferSize.set(inFlushSize.get());
  }

  public boolean requireReadMemoryWithRetry(long size) {
    ShuffleServerMetrics.counterTotalRequireReadMemoryNum.inc();
    for (int i = 0; i < retryNum; i++) {
      synchronized (this) {
        if (readDataMemory.get() + size < readCapacity) {
          readDataMemory.addAndGet(size);
          ShuffleServerMetrics.gaugeReadBufferUsedSize.inc(size);
          return true;
        }
      }
      LOG.info("Can't require[" + size + "] for read data, current[" + readDataMemory.get()
          + "], capacity[" + readCapacity + "], re-try " + i + " times");
      ShuffleServerMetrics.counterTotalRequireReadMemoryRetryNum.inc();
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
        LOG.warn("Error happened when require memory", e);
      }
    }
    ShuffleServerMetrics.counterTotalRequireReadMemoryFailedNum.inc();
    return false;
  }

  public void releaseReadMemory(long size) {
    if (readDataMemory.get() >= size) {
      readDataMemory.addAndGet(-size);
      ShuffleServerMetrics.gaugeReadBufferUsedSize.dec(size);
    } else {
      LOG.warn("Current read memory[" + readDataMemory.get()
          + "] is less than released[" + size + "], set read memory to 0");
      readDataMemory.set(0L);
      ShuffleServerMetrics.gaugeReadBufferUsedSize.set(0);
    }
  }

  // flush the buffer with required map which is <appId -> shuffleId>
  public synchronized void flush(Map<String, Set<Integer>> requiredFlush) {
    for (Map.Entry<String, Map<Integer, RangeMap<Integer, ShuffleBuffer>>>
        appIdToBuffers : bufferPool.entrySet()) {
      String appId = appIdToBuffers.getKey();
      if (requiredFlush.containsKey(appId)) {
        for (Map.Entry<Integer, RangeMap<Integer, ShuffleBuffer>> shuffleIdToBuffers :
            appIdToBuffers.getValue().entrySet()) {
          int shuffleId = shuffleIdToBuffers.getKey();
          Set<Integer> requiredShuffleId = requiredFlush.get(appId);
          if (requiredShuffleId != null && requiredShuffleId.contains(shuffleId)) {
            for (Map.Entry<Range<Integer>, ShuffleBuffer> rangeEntry :
                shuffleIdToBuffers.getValue().asMapOfRanges().entrySet()) {
              Range<Integer> range = rangeEntry.getKey();
              flushBuffer(rangeEntry.getValue(), appId, shuffleId,
                  range.lowerEndpoint(), range.upperEndpoint());
            }
          }
        }
      }
    }
  }

  public void updateUsedMemory(long delta) {
    // add size if not allocated
    usedMemory.addAndGet(delta);
    ShuffleServerMetrics.gaugeUsedBufferSize.set(usedMemory.get());
  }

  void requirePreAllocatedSize(long delta) {
    preAllocatedSize.addAndGet(delta);
    ShuffleServerMetrics.gaugeAllocatedBufferSize.set(preAllocatedSize.get());
  }

  public void releasePreAllocatedSize(long delta) {
    preAllocatedSize.addAndGet(-delta);
    ShuffleServerMetrics.gaugeAllocatedBufferSize.set(preAllocatedSize.get());
  }

  boolean isFull() {
    return usedMemory.get() >= capacity;
  }

  @VisibleForTesting
  public Map<String, Map<Integer, RangeMap<Integer, ShuffleBuffer>>> getBufferPool() {
    return bufferPool;
  }

  @VisibleForTesting
  public ShuffleBuffer getShuffleBuffer(String appId, int shuffleId, int partitionId) {
    return getShuffleBufferEntry(appId, shuffleId, partitionId).getValue();
  }

  public long getUsedMemory() {
    return usedMemory.get();
  }

  public long getInFlushSize() {
    return inFlushSize.get();
  }

  public long getCapacity() {
    return capacity;
  }

  @VisibleForTesting
  public long getReadCapacity() {
    return readCapacity;
  }

  @VisibleForTesting
  public void resetSize() {
    usedMemory = new AtomicLong(0L);
    preAllocatedSize = new AtomicLong(0L);
    inFlushSize = new AtomicLong(0L);
  }

  @VisibleForTesting
  public Map<String, Map<Integer, AtomicLong>> getShuffleSizeMap() {
    return shuffleSizeMap;
  }

  public long getPreAllocatedSize() {
    return preAllocatedSize.get();
  }

  // sort for shuffle according to data size, then pick properly data which will be flushed
  private Map<String, Set<Integer>> pickFlushedShuffle() {
    // create list for sort
    List<Entry<String, AtomicLong>> sizeList = generateSizeList();
    sizeList.sort((entry1, entry2) -> {
      if (entry1 == null && entry2 == null) {
        return 0;
      }
      if (entry1 == null) {
        return 1;
      }
      if (entry2 == null) {
        return -1;
      }
      if (entry1.getValue().get() > entry2.getValue().get()) {
        return -1;
      } else if (entry1.getValue().get() == entry2.getValue().get()) {
        return 0;
      }
      return 1;
    });

    Map<String, Set<Integer>> pickedShuffle = Maps.newHashMap();
    // The algorithm here is to flush data size > highWaterMark - lowWaterMark
    // the remaining data in buffer maybe more than lowWaterMark
    // because shuffle server is still receiving data, but it should be ok
    long expectedFlushSize = highWaterMark - lowWaterMark;
    long atLeastFlushSizeIgnoreThreshold = expectedFlushSize >>> 1;
    long pickedFlushSize = 0L;
    int printIndex = 0;
    int printIgnoreIndex = 0;
    int printMax = 10;
    for (Map.Entry<String, AtomicLong> entry : sizeList) {
      long size = entry.getValue().get();
      String appIdShuffleIdKey = entry.getKey();
      if (size > this.shuffleFlushThreshold || pickedFlushSize <= atLeastFlushSizeIgnoreThreshold) {
        pickedFlushSize += size;
        addPickedShuffle(appIdShuffleIdKey, pickedShuffle);
        // print detail picked info
        if (printIndex < printMax) {
          LOG.info("Pick application_shuffleId[{}] with {} bytes", appIdShuffleIdKey, size);
          printIndex++;
        }
        if (pickedFlushSize > expectedFlushSize) {
          LOG.info("Finish flush pick with {} bytes", pickedFlushSize);
          break;
        }
      } else {
        // since shuffle size is ordered by size desc, we can skip process more shuffle data once some shuffle's size
        // is less than threshold
        if (printIgnoreIndex < printMax) {
          LOG.info("Ignore application_shuffleId[{}] with {} bytes", appIdShuffleIdKey, size);
          printIgnoreIndex++;
        } else {
          break;
        }
      }
    }
    return pickedShuffle;
  }

  private List<Map.Entry<String, AtomicLong>> generateSizeList() {
    Map<String, AtomicLong> sizeMap = Maps.newHashMap();
    for (Map.Entry<String, Map<Integer, AtomicLong>> appEntry : shuffleSizeMap.entrySet()) {
      String appId = appEntry.getKey();
      for (Map.Entry<Integer, AtomicLong> shuffleEntry : appEntry.getValue().entrySet()) {
        Integer shuffleId = shuffleEntry.getKey();
        sizeMap.put(RssUtils.generateShuffleKey(appId, shuffleId), shuffleEntry.getValue());
      }
    }
    return Lists.newArrayList(sizeMap.entrySet());
  }

  private void addPickedShuffle(String key, Map<String, Set<Integer>> pickedShuffle) {
    String[] splits = key.split(Constants.KEY_SPLIT_CHAR);
    String appId = splits[0];
    Integer shuffleId = Integer.parseInt(splits[1]);
    pickedShuffle.putIfAbsent(appId, Sets.newHashSet());
    Set<Integer> shuffleIdSet = pickedShuffle.get(appId);
    shuffleIdSet.add(shuffleId);
  }

  public void removeBufferByShuffleId(String appId, Collection<Integer> shuffleIds) {
    Map<Integer, RangeMap<Integer, ShuffleBuffer>> shuffleIdToBuffers = bufferPool.get(appId);
    if (shuffleIdToBuffers == null) {
      return;
    }

    Map<Integer, AtomicLong> shuffleIdToSizeMap = shuffleSizeMap.get(appId);
    for (int shuffleId : shuffleIds) {
      long size = 0;

      RangeMap<Integer, ShuffleBuffer> bufferRangeMap = shuffleIdToBuffers.remove(shuffleId);
      if (bufferRangeMap == null) {
        continue;
      }
      Collection<ShuffleBuffer> buffers = bufferRangeMap.asMapOfRanges().values();
      if (buffers != null) {
        for (ShuffleBuffer buffer : buffers) {
          ShuffleServerMetrics.gaugeTotalPartitionNum.dec();
          size += buffer.getSize();
        }
      }
      releaseMemory(size, false, false);
      if (shuffleIdToSizeMap != null) {
        shuffleIdToSizeMap.remove(shuffleId);
      }
    }
  }

  public boolean isHugePartition(long usedPartitionDataSize) {
    return usedPartitionDataSize > hugePartitionSizeThreshold;
  }

  public boolean limitHugePartition(String appId, int shuffleId, int partitionId, long usedPartitionDataSize) {
    if (usedPartitionDataSize > hugePartitionSizeThreshold) {
      long memoryUsed = getShuffleBufferEntry(appId, shuffleId, partitionId).getValue().getSize();
      if (memoryUsed > hugePartitionMemoryLimitSize) {
        LOG.warn("AppId: {}, shuffleId: {}, partitionId: {}, memory used: {}, "
            + "huge partition triggered memory limitation.", appId, shuffleId, partitionId, memoryUsed);
        return true;
      }
    }
    return false;
  }
}
