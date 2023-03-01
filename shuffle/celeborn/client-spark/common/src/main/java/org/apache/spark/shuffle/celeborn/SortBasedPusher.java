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

package org.apache.spark.shuffle.celeborn;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import org.apache.spark.memory.MemoryConsumer;
import org.apache.spark.memory.SparkOutOfMemoryError;
import org.apache.spark.memory.TaskMemoryManager;
import org.apache.spark.memory.TooLargePageException;
import org.apache.spark.unsafe.Platform;
import org.apache.spark.unsafe.UnsafeAlignedOffset;
import org.apache.spark.unsafe.array.LongArray;
import org.apache.spark.unsafe.memory.MemoryBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.ShuffleClient;
import org.apache.celeborn.client.write.DataPusher;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.util.JavaUtils;
import org.apache.celeborn.common.util.Utils;

public class SortBasedPusher extends MemoryConsumer {

  private static final Logger logger = LoggerFactory.getLogger(SortBasedPusher.class);

  private ShuffleInMemorySorter inMemSorter;
  private final LinkedList<MemoryBlock> allocatedPages = new LinkedList<>();
  private MemoryBlock currentPage = null;
  private long pageCursor = -1;

  private final ShuffleClient rssShuffleClient;
  private final DataPusher dataPusher;
  private final int pushBufferMaxSize;
  private final long pushSortMemoryThreshold;
  final int uaoSize = UnsafeAlignedOffset.getUaoSize();
  static final long bytes8K = Utils.byteStringAsBytes("8k");

  String appId;
  int shuffleId;
  int mapId;
  int attemptNumber;
  long taskAttemptId;
  int numMappers;
  int numPartitions;
  CelebornConf conf;
  Consumer<Integer> afterPush;
  LongAdder[] mapStatusLengths;
  // this lock is shared between different SortBasedPushers to synchronize pushData
  Object sharedPushLock;
  volatile boolean asyncPushing = false;
  int[] shuffledPartitions = null;
  int[] inversedShuffledPartitions = null;
  ExecutorService executorService;

  public SortBasedPusher(
      TaskMemoryManager memoryManager,
      ShuffleClient rssShuffleClient,
      String appId,
      int shuffleId,
      int mapId,
      int attemptNumber,
      long taskAttemptId,
      int numMappers,
      int numPartitions,
      CelebornConf conf,
      Consumer<Integer> afterPush,
      LongAdder[] mapStatusLengths,
      long pushSortMemoryThreshold,
      Object sharedPushLock,
      ExecutorService executorService)
      throws IOException {
    super(
        memoryManager,
        (int) Math.min(PackedRecordPointer.MAXIMUM_PAGE_SIZE_BYTES, memoryManager.pageSizeBytes()),
        memoryManager.getTungstenMemoryMode());

    this.rssShuffleClient = rssShuffleClient;

    this.appId = appId;
    this.shuffleId = shuffleId;
    this.mapId = mapId;
    this.attemptNumber = attemptNumber;
    this.taskAttemptId = taskAttemptId;
    this.numMappers = numMappers;
    this.numPartitions = numPartitions;

    if (conf.pushSortRandomizePartitionIdEnabled()) {
      shuffledPartitions = new int[numPartitions];
      inversedShuffledPartitions = new int[numPartitions];
      JavaUtils.shuffleArray(shuffledPartitions, inversedShuffledPartitions);
    }

    this.conf = conf;
    this.afterPush = afterPush;
    this.mapStatusLengths = mapStatusLengths;

    dataPusher =
        new DataPusher(
            appId,
            shuffleId,
            mapId,
            attemptNumber,
            taskAttemptId,
            numMappers,
            numPartitions,
            conf,
            rssShuffleClient,
            afterPush,
            mapStatusLengths);

    pushBufferMaxSize = conf.pushBufferMaxSize();
    this.pushSortMemoryThreshold = pushSortMemoryThreshold;

    int initialSize = Math.min((int) pushSortMemoryThreshold / 8, 1024 * 1024);
    inMemSorter = new ShuffleInMemorySorter(this, initialSize);
    this.sharedPushLock = sharedPushLock;
    this.executorService = executorService;
  }

  public long pushData() throws IOException {
    // pushData should be synchronized between pushers
    synchronized (sharedPushLock) {
      final ShuffleInMemorySorter.ShuffleSorterIterator sortedRecords =
          inMemSorter.getSortedIterator();

      byte[] dataBuf = new byte[pushBufferMaxSize];
      int offSet = 0;
      int currentPartition = -1;
      while (sortedRecords.hasNext()) {
        sortedRecords.loadNext();
        final int partition =
            shuffledPartitions != null
                ? inversedShuffledPartitions[sortedRecords.packedRecordPointer.getPartitionId()]
                : sortedRecords.packedRecordPointer.getPartitionId();
        if (partition != currentPartition) {
          if (currentPartition == -1) {
            currentPartition = partition;
          } else {
            int bytesWritten =
                rssShuffleClient.mergeData(
                    appId,
                    shuffleId,
                    mapId,
                    attemptNumber,
                    currentPartition,
                    dataBuf,
                    0,
                    offSet,
                    numMappers,
                    numPartitions);
            mapStatusLengths[currentPartition].add(bytesWritten);
            afterPush.accept(bytesWritten);
            currentPartition = partition;
            offSet = 0;
          }
        }
        final long recordPointer = sortedRecords.packedRecordPointer.getRecordPointer();
        final Object recordPage = taskMemoryManager.getPage(recordPointer);
        final long recordOffsetInPage = taskMemoryManager.getOffsetInPage(recordPointer);
        int recordSize = UnsafeAlignedOffset.getSize(recordPage, recordOffsetInPage);

        if (offSet + recordSize > dataBuf.length) {
          dataPusher.addTask(partition, dataBuf, offSet);
          offSet = 0;
        }

        long recordReadPosition = recordOffsetInPage + uaoSize;
        Platform.copyMemory(
            recordPage,
            recordReadPosition,
            dataBuf,
            Platform.BYTE_ARRAY_OFFSET + offSet,
            recordSize);
        offSet += recordSize;
      }
      if (offSet > 0) {
        dataPusher.addTask(currentPartition, dataBuf, offSet);
      }

      long freedBytes = freeMemory();
      inMemSorter.freeMemory();

      return freedBytes;
    }
  }

  public boolean insertRecord(
      Object recordBase, long recordOffset, int recordSize, int partitionId, boolean copySize)
      throws IOException {

    if (getUsed() > pushSortMemoryThreshold
        && pageCursor + bytes8K > currentPage.getBaseOffset() + currentPage.size()) {
      logger.info(
          "Memory Used across threshold, need to trigger push. Memory: "
              + getUsed()
              + ", currentPage size: "
              + currentPage.size());
      return false;
    }

    int required;
    // Need 4 or 8 bytes to store the record recordSize.
    if (copySize) {
      required = recordSize + 4 + uaoSize;
    } else {
      required = recordSize + uaoSize;
    }
    allocateMemoryForRecordIfNecessary(required);

    assert (currentPage != null);
    final Object base = currentPage.getBaseObject();
    final long recordAddress = taskMemoryManager.encodePageNumberAndOffset(currentPage, pageCursor);
    if (copySize) {
      UnsafeAlignedOffset.putSize(base, pageCursor, recordSize + 4);
      pageCursor += uaoSize;
      Platform.putInt(base, pageCursor, Integer.reverseBytes(recordSize));
      pageCursor += 4;
      Platform.copyMemory(recordBase, recordOffset, base, pageCursor, recordSize);
      pageCursor += recordSize;
    } else {
      UnsafeAlignedOffset.putSize(base, pageCursor, recordSize);
      pageCursor += uaoSize;
      Platform.copyMemory(recordBase, recordOffset, base, pageCursor, recordSize);
      pageCursor += recordSize;
    }
    if (shuffledPartitions != null) {
      inMemSorter.insertRecord(recordAddress, shuffledPartitions[partitionId]);
    } else {
      inMemSorter.insertRecord(recordAddress, partitionId);
    }

    return true;
  }

  public void triggerPush() throws IOException {
    asyncPushing = true;
    dataPusher.checkException();
    executorService.submit(
        () -> {
          try {
            pushData();
            asyncPushing = false;
          } catch (IOException ie) {
            dataPusher.setException(ie);
          }
        });
  }

  /**
   * Since this method and pushData() are synchronized When this method returns, it means pushData
   * has released lock
   *
   * @throws IOException
   */
  public void waitPushFinish() throws IOException {
    dataPusher.checkException();
    while (asyncPushing) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        throw new IOException("Interrupted when waitPushFinish", e);
      }
    }
  }

  private void growPointerArrayIfNecessary() throws IOException {
    assert (inMemSorter != null);
    if (!inMemSorter.hasSpaceForAnotherRecord()) {
      if (inMemSorter.numRecords() <= 0) {
        // Spilling was triggered just before this method was called. The pointer array was freed
        // during the spill, so a new pointer array needs to be allocated here.
        LongArray array = allocateArray(inMemSorter.getInitialSize());
        inMemSorter.expandPointerArray(array);
        return;
      }

      long used = inMemSorter.getMemoryUsage();
      LongArray array = null;
      try {
        // could trigger spilling
        array = allocateArray(used / 8 * 2);
      } catch (TooLargePageException e) {
        // The pointer array is too big to fix in a single page, spill.
        logger.info("Pushdata in growPointerArrayIfNecessary, memory used " + getUsed());
        pushData();
      } catch (SparkOutOfMemoryError e) {
        // should have trigger spilling
        if (inMemSorter.numRecords() > 0) {
          logger.error("Unable to grow the pointer array");
          throw e;
        }
        // The new array could not be allocated, but that is not an issue as it is longer needed,
        // as all records were spilled.
      }

      if (inMemSorter.numRecords() <= 0) {
        // Spilling was triggered while trying to allocate the new array.
        if (array != null) {
          // We succeeded in allocating the new array, but, since all records were spilled, a
          // smaller array would also suffice.
          freeArray(array);
        }
        // The pointer array was freed during the spill, so a new pointer array needs to be
        // allocated here.
        array = allocateArray(inMemSorter.getInitialSize());
      }
      inMemSorter.expandPointerArray(array);
    }
  }

  /**
   * Allocates an additional page in order to insert an additional record. This will request
   * additional memory from the memory manager and spill if the requested memory can not be
   * obtained.
   *
   * @param required the required space in the data page, in bytes, including space for storing the
   *     record size.
   */
  private void acquireNewPageIfNecessary(int required) {
    if (currentPage == null
        || pageCursor + required > currentPage.getBaseOffset() + currentPage.size()) {
      currentPage = allocatePage(required);
      pageCursor = currentPage.getBaseOffset();
      allocatedPages.add(currentPage);
    }
  }

  /**
   * Allocates more memory in order to insert an additional record. This will request additional
   * memory from the memory manager and spill if the requested memory can not be obtained.
   *
   * @param required the required space in the data page, in bytes, including space for storing the
   *     record size.
   */
  private void allocateMemoryForRecordIfNecessary(int required) throws IOException {
    // Step 1:
    // Ensure that the pointer array has space for another record. This may cause a spill.
    growPointerArrayIfNecessary();
    // Step 2:
    // Ensure that the last page has space for another record. This may cause a spill.
    acquireNewPageIfNecessary(required);
    // Step 3:
    // The allocation in step 2 could have caused a spill, which would have freed the pointer
    // array allocated in step 1. Therefore we need to check again whether we have to allocate
    // a new pointer array.
    //
    // If the allocation in this step causes a spill event then it will not cause the page
    // allocated in the previous step to be freed. The function `spill` only frees memory if at
    // least one record has been inserted in the in-memory sorter. This will not be the case if
    // we have spilled in the previous step.
    //
    // If we did not spill in the previous step then `growPointerArrayIfNecessary` will be a
    // no-op that does not allocate any memory, and therefore can't cause a spill event.
    //
    // Thus there is no need to call `acquireNewPageIfNecessary` again after this step.
    growPointerArrayIfNecessary();
  }

  @Override
  public long spill(long l, MemoryConsumer memoryConsumer) throws IOException {
    logger.warn("SortBasedPusher not support spill yet");
    return 0;
  }

  private long freeMemory() {
    long memoryFreed = 0;
    for (MemoryBlock block : allocatedPages) {
      memoryFreed += block.size();
      freePage(block);
    }
    allocatedPages.clear();
    currentPage = null;
    pageCursor = 0;
    return memoryFreed;
  }

  public void cleanupResources() {
    freeMemory();
    if (inMemSorter != null) {
      inMemSorter.freeMemory();
      inMemSorter = null;
    }
  }

  public void close() throws IOException {
    cleanupResources();
    dataPusher.waitOnTermination();
  }

  public long getUsed() {
    return super.getUsed();
  }
}
