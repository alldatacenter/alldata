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

package org.apache.uniffle.client.impl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.apache.hadoop.conf.Configuration;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.ShuffleReadClient;
import org.apache.uniffle.client.response.CompressedShuffleBlock;
import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.ChecksumUtils;
import org.apache.uniffle.common.util.IdHelper;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.storage.factory.ShuffleHandlerFactory;
import org.apache.uniffle.storage.handler.api.ClientReadHandler;
import org.apache.uniffle.storage.request.CreateShuffleReadHandlerRequest;

public class ShuffleReadClientImpl implements ShuffleReadClient {

  private static final Logger LOG = LoggerFactory.getLogger(ShuffleReadClientImpl.class);
  private final List<ShuffleServerInfo> shuffleServerInfoList;
  private int shuffleId;
  private int partitionId;
  private byte[] readBuffer;
  private Roaring64NavigableMap blockIdBitmap;
  private Roaring64NavigableMap taskIdBitmap;
  private Roaring64NavigableMap pendingBlockIds;
  private Roaring64NavigableMap processedBlockIds = Roaring64NavigableMap.bitmapOf();
  private Queue<BufferSegment> bufferSegmentQueue = Queues.newLinkedBlockingQueue();
  private AtomicLong readDataTime = new AtomicLong(0);
  private AtomicLong copyTime = new AtomicLong(0);
  private AtomicLong crcCheckTime = new AtomicLong(0);
  private ClientReadHandler clientReadHandler;
  private final IdHelper idHelper;

  public ShuffleReadClientImpl(
      String storageType,
      String appId,
      int shuffleId,
      int partitionId,
      int indexReadLimit,
      int partitionNumPerRange,
      int partitionNum,
      int readBufferSize,
      String storageBasePath,
      Roaring64NavigableMap blockIdBitmap,
      Roaring64NavigableMap taskIdBitmap,
      List<ShuffleServerInfo> shuffleServerInfoList,
      Configuration hadoopConf,
      IdHelper idHelper,
      ShuffleDataDistributionType dataDistributionType,
      boolean expectedTaskIdsBitmapFilterEnable) {
    this.shuffleId = shuffleId;
    this.partitionId = partitionId;
    this.blockIdBitmap = blockIdBitmap;
    this.taskIdBitmap = taskIdBitmap;
    this.idHelper = idHelper;
    this.shuffleServerInfoList = shuffleServerInfoList;

    CreateShuffleReadHandlerRequest request = new CreateShuffleReadHandlerRequest();
    request.setStorageType(storageType);
    request.setAppId(appId);
    request.setShuffleId(shuffleId);
    request.setPartitionId(partitionId);
    request.setIndexReadLimit(indexReadLimit);
    request.setPartitionNumPerRange(partitionNumPerRange);
    request.setPartitionNum(partitionNum);
    request.setReadBufferSize(readBufferSize);
    request.setStorageBasePath(storageBasePath);
    request.setShuffleServerInfoList(shuffleServerInfoList);
    request.setHadoopConf(hadoopConf);
    request.setExpectBlockIds(blockIdBitmap);
    request.setProcessBlockIds(processedBlockIds);
    request.setDistributionType(dataDistributionType);
    request.setIdHelper(idHelper);
    request.setExpectTaskIds(taskIdBitmap);
    if (expectedTaskIdsBitmapFilterEnable) {
      request.useExpectedTaskIdsBitmapFilter();
    }

    List<Long> removeBlockIds = Lists.newArrayList();
    blockIdBitmap.forEach(bid -> {
      if (!taskIdBitmap.contains(idHelper.getTaskAttemptId(bid))) {
        removeBlockIds.add(bid);
      }
    });

    for (long rid : removeBlockIds) {
      blockIdBitmap.removeLong(rid);
    }

    // copy blockIdBitmap to track all pending blocks
    pendingBlockIds = RssUtils.cloneBitMap(blockIdBitmap);

    clientReadHandler = ShuffleHandlerFactory.getInstance().createShuffleReadHandler(request);
  }

  public ShuffleReadClientImpl(
      String storageType,
      String appId,
      int shuffleId,
      int partitionId,
      int indexReadLimit,
      int partitionNumPerRange,
      int partitionNum,
      int readBufferSize,
      String storageBasePath,
      Roaring64NavigableMap blockIdBitmap,
      Roaring64NavigableMap taskIdBitmap,
      List<ShuffleServerInfo> shuffleServerInfoList,
      Configuration hadoopConf,
      IdHelper idHelper) {
    this(storageType, appId, shuffleId, partitionId, indexReadLimit,
        partitionNumPerRange, partitionNum, readBufferSize, storageBasePath,
        blockIdBitmap, taskIdBitmap, shuffleServerInfoList, hadoopConf,
        idHelper, ShuffleDataDistributionType.NORMAL, false);
  }

  @Override
  public CompressedShuffleBlock readShuffleBlockData() {
    // empty data expected, just return null
    if (blockIdBitmap.isEmpty()) {
      return null;
    }

    // All blocks are processed, so just return
    if (pendingBlockIds.isEmpty()) {
      return null;
    }

    // if client need request new data from shuffle server
    if (bufferSegmentQueue.isEmpty()) {
      if (read() <= 0) {
        return null;
      }
    }

    // get next buffer segment
    BufferSegment bs = null;

    // blocks in bufferSegmentQueue may be from different partition in range partition mode,
    // or may be from speculation task, filter them and just read the necessary block
    while (true) {
      bs = bufferSegmentQueue.poll();
      if (bs == null) {
        break;
      }
      // check 1: if blockId is processed
      // check 2: if blockId is required for current partition
      // check 3: if blockId is generated by required task
      if (!processedBlockIds.contains(bs.getBlockId())
          && blockIdBitmap.contains(bs.getBlockId())
          && taskIdBitmap.contains(bs.getTaskAttemptId())) {
        long expectedCrc = -1;
        long actualCrc = -1;
        try {
          long start = System.currentTimeMillis();
          expectedCrc = bs.getCrc();
          actualCrc = ChecksumUtils.getCrc32(readBuffer, bs.getOffset(), bs.getLength());
          crcCheckTime.addAndGet(System.currentTimeMillis() - start);
        } catch (Exception e) {
          LOG.warn("Can't read data for blockId[" + bs.getBlockId() + "]", e);
        }

        if (expectedCrc != actualCrc) {
          String errMsg = "Unexpected crc value for blockId[" + bs.getBlockId()
              + "], expected:" + expectedCrc + ", actual:" + actualCrc;
          //If some blocks of one replica are corrupted,but maybe other replicas are not corrupted,
          //so exception should not be thrown here if blocks have multiple replicas
          if (shuffleServerInfoList.size() > 1) {
            LOG.warn(errMsg);
            clientReadHandler.updateConsumedBlockInfo(bs, true);
            continue;
          } else {
            throw new RssException(errMsg);
          }
        }

        // mark block as processed
        processedBlockIds.addLong(bs.getBlockId());
        pendingBlockIds.removeLong(bs.getBlockId());
        // only update the statistics of necessary blocks
        clientReadHandler.updateConsumedBlockInfo(bs, false);
        break;
      }
      clientReadHandler.updateConsumedBlockInfo(bs, true);
      // mark block as processed
      processedBlockIds.addLong(bs.getBlockId());
      pendingBlockIds.removeLong(bs.getBlockId());
    }

    if (bs != null) {
      return new CompressedShuffleBlock(ByteBuffer.wrap(readBuffer,
          bs.getOffset(), bs.getLength()), bs.getUncompressLength());
    }
    // current segment hasn't data, try next segment
    return readShuffleBlockData();
  }

  @VisibleForTesting
  protected Roaring64NavigableMap getProcessedBlockIds() {
    return processedBlockIds;
  }

  private int read() {
    long start = System.currentTimeMillis();
    ShuffleDataResult sdr = clientReadHandler.readShuffleData();
    readDataTime.addAndGet(System.currentTimeMillis() - start);
    if (sdr == null) {
      return 0;
    }
    readBuffer = sdr.getData();
    if (readBuffer == null || readBuffer.length == 0) {
      return 0;
    }
    bufferSegmentQueue.addAll(sdr.getBufferSegments());
    return sdr.getBufferSegments().size();
  }

  @Override
  public void checkProcessedBlockIds() {
    RssUtils.checkProcessedBlockIds(blockIdBitmap, processedBlockIds);
  }

  @Override
  public void close() {
    if (clientReadHandler != null) {
      clientReadHandler.close();
    }
  }

  @Override
  public void logStatics() {
    LOG.info("Metrics for shuffleId[" + shuffleId + "], partitionId[" + partitionId + "]"
        + ", read data cost " + readDataTime + " ms, copy data cost " + copyTime
        + " ms, crc check cost " + crcCheckTime + " ms");
    clientReadHandler.logConsumedBlockInfo();
  }
}
