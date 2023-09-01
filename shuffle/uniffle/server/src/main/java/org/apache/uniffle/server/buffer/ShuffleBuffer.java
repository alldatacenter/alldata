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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.common.ShufflePartitionedData;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.server.ShuffleDataFlushEvent;
import org.apache.uniffle.server.ShuffleFlushManager;

public class ShuffleBuffer {

  private static final Logger LOG = LoggerFactory.getLogger(ShuffleBuffer.class);

  private final long capacity;
  private long size;
  // blocks will be added to inFlushBlockMap as <eventId, blocks> pair
  // it will be removed after flush to storage
  // the strategy ensure that shuffle is in memory or storage
  private List<ShufflePartitionedBlock> blocks;
  private Map<Long, List<ShufflePartitionedBlock>> inFlushBlockMap;

  public ShuffleBuffer(long capacity) {
    this.capacity = capacity;
    this.size = 0;
    this.blocks = new LinkedList<>();
    this.inFlushBlockMap = Maps.newConcurrentMap();
  }

  public long append(ShufflePartitionedData data) {
    long mSize = 0;

    synchronized (this) {
      for (ShufflePartitionedBlock block : data.getBlockList()) {
        blocks.add(block);
        mSize += block.getSize();
      }
      size += mSize;
    }

    return mSize;
  }

  public synchronized ShuffleDataFlushEvent toFlushEvent(
      String appId,
      int shuffleId,
      int startPartition,
      int endPartition,
      Supplier<Boolean> isValid,
      ShuffleDataDistributionType dataDistributionType) {
    if (blocks.isEmpty()) {
      return null;
    }
    // buffer will be cleared, and new list must be created for async flush
    List<ShufflePartitionedBlock> spBlocks = new LinkedList<>(blocks);
    List<ShufflePartitionedBlock> inFlushedQueueBlocks = spBlocks;
    if (dataDistributionType == ShuffleDataDistributionType.LOCAL_ORDER) {
      /**
       * When reordering the blocks, it will break down the original reads sequence to cause
       * the data lost in some cases.
       * So we should create a reference copy to avoid this.
       */
      inFlushedQueueBlocks = new LinkedList<>(spBlocks);
      spBlocks.sort(Comparator.comparingLong(ShufflePartitionedBlock::getTaskAttemptId));
    }
    long eventId = ShuffleFlushManager.ATOMIC_EVENT_ID.getAndIncrement();
    final ShuffleDataFlushEvent event = new ShuffleDataFlushEvent(
        eventId,
        appId,
        shuffleId,
        startPartition,
        endPartition,
        size,
        spBlocks,
        isValid,
        this);
    event.addCleanupCallback(() -> this.clearInFlushBuffer(event.getEventId()));
    inFlushBlockMap.put(eventId, inFlushedQueueBlocks);
    blocks.clear();
    size = 0;
    return event;
  }

  /**
   * Only for test
   */
  public synchronized ShuffleDataFlushEvent toFlushEvent(
      String appId,
      int shuffleId,
      int startPartition,
      int endPartition,
      Supplier<Boolean> isValid) {
    return toFlushEvent(appId, shuffleId, startPartition, endPartition, isValid, ShuffleDataDistributionType.NORMAL);
  }

  public List<ShufflePartitionedBlock> getBlocks() {
    return blocks;
  }

  public long getSize() {
    return size;
  }

  public boolean isFull() {
    return size > capacity;
  }

  public synchronized void clearInFlushBuffer(long eventId) {
    inFlushBlockMap.remove(eventId);
  }

  @VisibleForTesting
  public Map<Long, List<ShufflePartitionedBlock>> getInFlushBlockMap() {
    return inFlushBlockMap;
  }

  public synchronized ShuffleDataResult getShuffleData(
      long lastBlockId, int readBufferSize) {
    return getShuffleData(lastBlockId, readBufferSize, null);
  }

  // 1. generate buffer segments and other info: if blockId exist, start with which eventId
  // 2. according to info from step 1, generate data
  // todo: if block was flushed, it's possible to get duplicated data
  public synchronized ShuffleDataResult getShuffleData(
      long lastBlockId, int readBufferSize, Roaring64NavigableMap expectedTaskIds) {
    try {
      List<BufferSegment> bufferSegments = Lists.newArrayList();
      List<ShufflePartitionedBlock> readBlocks = Lists.newArrayList();
      updateBufferSegmentsAndResultBlocks(
          lastBlockId, readBufferSize, bufferSegments, readBlocks, expectedTaskIds);
      if (!bufferSegments.isEmpty()) {
        int length = calculateDataLength(bufferSegments);
        byte[] data = new byte[length];
        // copy result data
        updateShuffleData(readBlocks, data);
        return new ShuffleDataResult(data, bufferSegments);
      }
    } catch (Exception e) {
      LOG.error("Exception happened when getShuffleData in buffer", e);
    }
    return new ShuffleDataResult();
  }

  // here is the rule to read data in memory:
  // 1. read from inFlushBlockMap order by eventId asc, then from blocks
  // 2. if can't find lastBlockId, means related data may be flushed to storage, repeat step 1
  private void updateBufferSegmentsAndResultBlocks(
      long lastBlockId,
      long readBufferSize,
      List<BufferSegment> bufferSegments,
      List<ShufflePartitionedBlock> resultBlocks,
      Roaring64NavigableMap expectedTaskIds) {
    long nextBlockId = lastBlockId;
    List<Long> sortedEventId = sortFlushingEventId();
    int offset = 0;
    boolean hasLastBlockId = false;
    // read from inFlushBlockMap first to make sure the order of
    // data read is according to the order of data received
    // The number of events means how many batches are in flushing status,
    // it should be less than 5, or there has some problem with storage
    if (!inFlushBlockMap.isEmpty()) {
      for (Long eventId : sortedEventId) {
        // update bufferSegments with different strategy according to lastBlockId
        if (nextBlockId == Constants.INVALID_BLOCK_ID) {
          updateSegmentsWithoutBlockId(offset, inFlushBlockMap.get(eventId), readBufferSize,
              bufferSegments, resultBlocks, expectedTaskIds);
          hasLastBlockId = true;
        } else {
          hasLastBlockId = updateSegmentsWithBlockId(offset, inFlushBlockMap.get(eventId),
              readBufferSize, nextBlockId, bufferSegments, resultBlocks, expectedTaskIds);
          // if last blockId is found, read from begin with next cached blocks
          if (hasLastBlockId) {
            // reset blockId to read from begin in next cached blocks
            nextBlockId = Constants.INVALID_BLOCK_ID;
          }
        }
        if (!bufferSegments.isEmpty()) {
          offset = calculateDataLength(bufferSegments);
        }
        if (offset >= readBufferSize) {
          break;
        }
      }
    }
    // try to read from cached blocks which is not in flush queue
    if (blocks.size() > 0 && offset < readBufferSize) {
      if (nextBlockId == Constants.INVALID_BLOCK_ID) {
        updateSegmentsWithoutBlockId(offset, blocks, readBufferSize, bufferSegments, resultBlocks, expectedTaskIds);
        hasLastBlockId = true;
      } else {
        hasLastBlockId = updateSegmentsWithBlockId(offset, blocks,
            readBufferSize, nextBlockId, bufferSegments, resultBlocks, expectedTaskIds);
      }
    }
    if ((!inFlushBlockMap.isEmpty() || blocks.size() > 0) && offset == 0 && !hasLastBlockId) {
      // can't find lastBlockId, it should be flushed
      // but there still has data in memory
      // try read again with blockId = Constants.INVALID_BLOCK_ID
      updateBufferSegmentsAndResultBlocks(
          Constants.INVALID_BLOCK_ID, readBufferSize, bufferSegments, resultBlocks, expectedTaskIds);
    }
  }

  private int calculateDataLength(List<BufferSegment> bufferSegments) {
    BufferSegment bufferSegment = bufferSegments.get(bufferSegments.size() - 1);
    return bufferSegment.getOffset() + bufferSegment.getLength();
  }

  private void updateShuffleData(List<ShufflePartitionedBlock> readBlocks, byte[] data) {
    int offset = 0;
    for (ShufflePartitionedBlock block : readBlocks) {
      // fill shuffle data
      try {
        System.arraycopy(block.getData(), 0, data, offset, block.getLength());
      } catch (Exception e) {
        LOG.error("Unexpected exception for System.arraycopy, length["
            + block.getLength() + "], offset["
            + offset + "], dataLength[" + data.length + "]", e);
        throw e;
      }
      offset += block.getLength();
    }
  }

  private List<Long> sortFlushingEventId() {
    List<Long> eventIdList = Lists.newArrayList(inFlushBlockMap.keySet());
    eventIdList.sort((id1, id2) -> {
      if (id1 > id2) {
        return 1;
      }
      return -1;
    });
    return eventIdList;
  }

  private void updateSegmentsWithoutBlockId(
      int offset,
      List<ShufflePartitionedBlock> cachedBlocks,
      long readBufferSize,
      List<BufferSegment> bufferSegments,
      List<ShufflePartitionedBlock> readBlocks,
      Roaring64NavigableMap expectedTaskIds) {
    int currentOffset = offset;
    // read from first block
    for (ShufflePartitionedBlock block : cachedBlocks) {
      if (expectedTaskIds != null && !expectedTaskIds.contains(block.getTaskAttemptId())) {
        continue;
      }
      // add bufferSegment with block
      bufferSegments.add(new BufferSegment(block.getBlockId(), currentOffset, block.getLength(),
          block.getUncompressLength(), block.getCrc(), block.getTaskAttemptId()));
      readBlocks.add(block);
      // update offset
      currentOffset += block.getLength();
      // check if length >= request buffer size
      if (currentOffset >= readBufferSize) {
        break;
      }
    }
  }

  private boolean updateSegmentsWithBlockId(
      int offset,
      List<ShufflePartitionedBlock> cachedBlocks,
      long readBufferSize,
      long lastBlockId,
      List<BufferSegment> bufferSegments,
      List<ShufflePartitionedBlock> readBlocks,
      Roaring64NavigableMap expectedTaskIds) {
    int currentOffset = offset;
    // find lastBlockId, then read from next block
    boolean foundBlockId = false;
    for (ShufflePartitionedBlock block : cachedBlocks) {
      if (!foundBlockId) {
        // find lastBlockId
        if (block.getBlockId() == lastBlockId) {
          foundBlockId = true;
        }
        continue;
      }
      if (expectedTaskIds != null && !expectedTaskIds.contains(block.getTaskAttemptId())) {
        continue;
      }
      // add bufferSegment with block
      bufferSegments.add(new BufferSegment(block.getBlockId(), currentOffset, block.getLength(),
          block.getUncompressLength(), block.getCrc(), block.getTaskAttemptId()));
      readBlocks.add(block);
      // update offset
      currentOffset += block.getLength();
      if (currentOffset >= readBufferSize) {
        break;
      }
    }
    return foundBlockId;
  }
}
