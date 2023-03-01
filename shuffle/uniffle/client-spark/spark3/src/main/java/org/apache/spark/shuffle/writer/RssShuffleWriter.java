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

package org.apache.spark.shuffle.writer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.spark.Partitioner;
import org.apache.spark.ShuffleDependency;
import org.apache.spark.SparkConf;
import org.apache.spark.executor.ShuffleWriteMetrics;
import org.apache.spark.scheduler.MapStatus;
import org.apache.spark.shuffle.RssShuffleHandle;
import org.apache.spark.shuffle.RssShuffleManager;
import org.apache.spark.shuffle.RssSparkConfig;
import org.apache.spark.shuffle.ShuffleWriter;
import org.apache.spark.storage.BlockManagerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Function1;
import scala.Option;
import scala.Product2;
import scala.collection.Iterator;

import org.apache.uniffle.client.api.ShuffleWriteClient;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.storage.util.StorageType;

public class RssShuffleWriter<K, V, C> extends ShuffleWriter<K, V> {

  private static final Logger LOG = LoggerFactory.getLogger(RssShuffleWriter.class);
  private static final String DUMMY_HOST = "dummy_host";
  private static final int DUMMY_PORT = 99999;

  private final String appId;
  private final int shuffleId;
  private final WriteBufferManager bufferManager;
  private final String taskId;
  private final long taskAttemptId;
  private final int numMaps;
  private final ShuffleDependency<K, V, C> shuffleDependency;
  private final ShuffleWriteMetrics shuffleWriteMetrics;
  private final Partitioner partitioner;
  private final RssShuffleManager shuffleManager;
  private final boolean shouldPartition;
  private final long sendCheckTimeout;
  private final long sendCheckInterval;
  private final long sendSizeLimit;
  private final int bitmapSplitNum;
  private final Map<Integer, Set<Long>> partitionToBlockIds;
  private final ShuffleWriteClient shuffleWriteClient;
  private final Map<Integer, List<ShuffleServerInfo>> partitionToServers;
  private final Set<ShuffleServerInfo> shuffleServersForData;
  private final long[] partitionLengths;
  private boolean isMemoryShuffleEnabled;
  private final Function<String, Boolean> taskFailureCallback;

  public RssShuffleWriter(
      String appId,
      int shuffleId,
      String taskId,
      long taskAttemptId,
      WriteBufferManager bufferManager,
      ShuffleWriteMetrics shuffleWriteMetrics,
      RssShuffleManager shuffleManager,
      SparkConf sparkConf,
      ShuffleWriteClient shuffleWriteClient,
      RssShuffleHandle<K, V, C> rssHandle) {
    this(
        appId,
        shuffleId,
        taskId,
        taskAttemptId,
        bufferManager,
        shuffleWriteMetrics,
        shuffleManager,
        sparkConf,
        shuffleWriteClient,
        rssHandle,
        (tid) -> true
    );
  }

  public RssShuffleWriter(
      String appId,
      int shuffleId,
      String taskId,
      long taskAttemptId,
      WriteBufferManager bufferManager,
      ShuffleWriteMetrics shuffleWriteMetrics,
      RssShuffleManager shuffleManager,
      SparkConf sparkConf,
      ShuffleWriteClient shuffleWriteClient,
      RssShuffleHandle<K, V, C> rssHandle,
      Function<String, Boolean> taskFailureCallback) {
    LOG.warn("RssShuffle start write taskAttemptId data" + taskAttemptId);
    this.shuffleManager = shuffleManager;
    this.appId = appId;
    this.bufferManager = bufferManager;
    this.shuffleId = shuffleId;
    this.taskId = taskId;
    this.taskAttemptId = taskAttemptId;
    this.numMaps = rssHandle.getNumMaps();
    this.shuffleWriteMetrics = shuffleWriteMetrics;
    this.shuffleDependency = rssHandle.getDependency();
    this.partitioner = shuffleDependency.partitioner();
    this.shouldPartition = partitioner.numPartitions() > 1;
    this.sendCheckTimeout = sparkConf.get(RssSparkConfig.RSS_CLIENT_SEND_CHECK_TIMEOUT_MS);
    this.sendCheckInterval = sparkConf.get(RssSparkConfig.RSS_CLIENT_SEND_CHECK_INTERVAL_MS);
    this.sendSizeLimit = sparkConf.getSizeAsBytes(RssSparkConfig.RSS_CLIENT_SEND_SIZE_LIMIT.key(),
        RssSparkConfig.RSS_CLIENT_SEND_SIZE_LIMIT.defaultValue().get());
    this.bitmapSplitNum = sparkConf.get(RssSparkConfig.RSS_CLIENT_BITMAP_SPLIT_NUM);
    this.partitionToBlockIds = Maps.newHashMap();
    this.shuffleWriteClient = shuffleWriteClient;
    this.shuffleServersForData = rssHandle.getShuffleServersForData();
    this.partitionLengths = new long[partitioner.numPartitions()];
    Arrays.fill(partitionLengths, 0);
    partitionToServers = rssHandle.getPartitionToServers();
    this.isMemoryShuffleEnabled = isMemoryShuffleEnabled(
        sparkConf.get(RssSparkConfig.RSS_STORAGE_TYPE.key()));
    this.taskFailureCallback = taskFailureCallback;
  }

  private boolean isMemoryShuffleEnabled(String storageType) {
    return StorageType.withMemory(StorageType.valueOf(storageType));
  }

  @Override
  public void write(Iterator<Product2<K, V>> records) throws IOException {
    try {
      writeImpl(records);
    } catch (Exception e) {
      taskFailureCallback.apply(taskId);
      throw e;
    }
  }

  private void writeImpl(Iterator<Product2<K,V>> records) {
    List<ShuffleBlockInfo> shuffleBlockInfos;
    Set<Long> blockIds = Sets.newHashSet();
    boolean isCombine = shuffleDependency.mapSideCombine();
    Function1<V, C> createCombiner = null;
    if (isCombine) {
      createCombiner = shuffleDependency.aggregator().get().createCombiner();
    }
    while (records.hasNext()) {
      // Task should fast fail when sending data failed
      checkIfBlocksFailed();

      Product2<K, V> record = records.next();
      K key = record._1();
      int partition = getPartition(key);
      if (isCombine) {
        Object c = createCombiner.apply(record._2());
        shuffleBlockInfos = bufferManager.addRecord(partition, record._1(), c);
      } else {
        shuffleBlockInfos = bufferManager.addRecord(partition, record._1(), record._2());
      }
      if (shuffleBlockInfos != null && !shuffleBlockInfos.isEmpty()) {
        processShuffleBlockInfos(shuffleBlockInfos, blockIds);
      }
    }
    final long start = System.currentTimeMillis();
    shuffleBlockInfos = bufferManager.clear();
    if (shuffleBlockInfos != null && !shuffleBlockInfos.isEmpty()) {
      processShuffleBlockInfos(shuffleBlockInfos, blockIds);
    }
    long checkStartTs = System.currentTimeMillis();
    checkBlockSendResult(blockIds);
    long commitStartTs = System.currentTimeMillis();
    long checkDuration = commitStartTs - checkStartTs;
    if (!isMemoryShuffleEnabled) {
      sendCommit();
    }
    long writeDurationMs = bufferManager.getWriteTime() + (System.currentTimeMillis() - start);
    shuffleWriteMetrics.incWriteTime(TimeUnit.MILLISECONDS.toNanos(writeDurationMs));
    LOG.info("Finish write shuffle for appId[" + appId + "], shuffleId[" + shuffleId
        + "], taskId[" + taskId + "] with write " + writeDurationMs + " ms, include checkSendResult["
        + checkDuration + "], commit[" + (System.currentTimeMillis() - commitStartTs) + "], "
        + bufferManager.getManagerCostInfo());
  }

  // only push-based shuffle use this interface, but rss won't be used when push-based shuffle is enabled.
  public long[] getPartitionLengths() {
    return new long[0];
  }

  private void processShuffleBlockInfos(List<ShuffleBlockInfo> shuffleBlockInfoList, Set<Long> blockIds) {
    if (shuffleBlockInfoList != null && !shuffleBlockInfoList.isEmpty()) {
      shuffleBlockInfoList.forEach(sbi -> {
        long blockId = sbi.getBlockId();
        // add blockId to set, check if it is sent later
        blockIds.add(blockId);
        // update [partition, blockIds], it will be sent to shuffle server
        int partitionId = sbi.getPartitionId();
        partitionToBlockIds.computeIfAbsent(partitionId, k -> Sets.newHashSet()).add(blockId);
        partitionLengths[partitionId] += sbi.getLength();
      });
      postBlockEvent(shuffleBlockInfoList);
    }
  }

  protected void postBlockEvent(List<ShuffleBlockInfo> shuffleBlockInfoList) {
    long totalSize = 0;
    List<ShuffleBlockInfo> shuffleBlockInfosPerEvent = Lists.newArrayList();
    for (ShuffleBlockInfo sbi : shuffleBlockInfoList) {
      totalSize += sbi.getSize();
      shuffleBlockInfosPerEvent.add(sbi);
      // split shuffle data according to the size
      if (totalSize > sendSizeLimit) {
        LOG.info("Post event to queue with " + shuffleBlockInfosPerEvent.size()
            + " blocks and " + totalSize + " bytes");
        shuffleManager.postEvent(
            new AddBlockEvent(taskId, shuffleBlockInfosPerEvent));
        shuffleBlockInfosPerEvent = Lists.newArrayList();
        totalSize = 0;
      }
    }
    if (!shuffleBlockInfosPerEvent.isEmpty()) {
      LOG.info("Post event to queue with " + shuffleBlockInfosPerEvent.size()
          + " blocks and " + totalSize + " bytes");
      shuffleManager.postEvent(
          new AddBlockEvent(taskId, shuffleBlockInfosPerEvent));
    }
  }

  @VisibleForTesting
  protected void checkBlockSendResult(Set<Long> blockIds) {
    long start = System.currentTimeMillis();
    while (true) {
      checkIfBlocksFailed();
      Set<Long> successBlockIds = shuffleManager.getSuccessBlockIds(taskId);
      blockIds.removeAll(successBlockIds);
      if (blockIds.isEmpty()) {
        break;
      }
      LOG.info("Wait " + blockIds.size() + " blocks sent to shuffle server");
      Uninterruptibles.sleepUninterruptibly(sendCheckInterval, TimeUnit.MILLISECONDS);
      if (System.currentTimeMillis() - start > sendCheckTimeout) {
        String errorMsg = "Timeout: Task[" + taskId + "] failed because " + blockIds.size()
            + " blocks can't be sent to shuffle server in " + sendCheckTimeout + " ms.";
        LOG.error(errorMsg);
        throw new RssException(errorMsg);
      }
    }
  }

  private void checkIfBlocksFailed() {
    Set<Long> failedBlockIds = shuffleManager.getFailedBlockIds(taskId);
    if (!failedBlockIds.isEmpty()) {
      String errorMsg = "Send failed: Task[" + taskId + "]"
          + " failed because " + failedBlockIds.size()
          + " blocks can't be sent to shuffle server.";
      LOG.error(errorMsg);
      throw new RssException(errorMsg);
    }
  }

  @VisibleForTesting
  protected void sendCommit() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<Boolean> future = executor.submit(
        () -> shuffleWriteClient.sendCommit(shuffleServersForData, appId, shuffleId, numMaps));
    int maxWait = 5000;
    int currentWait = 200;
    long start = System.currentTimeMillis();
    while (!future.isDone()) {
      LOG.info("Wait commit to shuffle server for task[" + taskAttemptId + "] cost "
          + (System.currentTimeMillis() - start) + " ms");
      Uninterruptibles.sleepUninterruptibly(currentWait, TimeUnit.MILLISECONDS);
      currentWait = Math.min(currentWait * 2, maxWait);
    }
    try {
      if (!future.get()) {
        throw new RssException("Failed to commit task to shuffle server");
      }
    } catch (InterruptedException ie) {
      LOG.warn("Ignore the InterruptedException which should be caused by internal killed");
    } catch (Exception e) {
      throw new RuntimeException("Exception happened when get commit status", e);
    } finally {
      executor.shutdown();
    }
  }

  @VisibleForTesting
  protected <T> int getPartition(T key) {
    int result = 0;
    if (shouldPartition) {
      result = partitioner.getPartition(key);
    }
    return result;
  }

  @Override
  public Option<MapStatus> stop(boolean success) {
    try {
      if (success) {
        Map<Integer, List<Long>> ptb = Maps.newHashMap();
        for (Map.Entry<Integer, Set<Long>> entry : partitionToBlockIds.entrySet()) {
          ptb.put(entry.getKey(), Lists.newArrayList(entry.getValue()));
        }
        long start = System.currentTimeMillis();
        shuffleWriteClient.reportShuffleResult(partitionToServers, appId, shuffleId,
            taskAttemptId, ptb, bitmapSplitNum);
        LOG.info("Report shuffle result for task[{}] with bitmapNum[{}] cost {} ms",
            taskAttemptId, bitmapSplitNum, (System.currentTimeMillis() - start));
        // todo: we can replace the dummy host and port with the real shuffle server which we prefer to read
        final BlockManagerId blockManagerId = BlockManagerId.apply(appId + "_" + taskId,
            DUMMY_HOST,
            DUMMY_PORT,
            Option.apply(Long.toString(taskAttemptId)));
        MapStatus mapStatus = MapStatus.apply(blockManagerId, partitionLengths, taskAttemptId);
        return Option.apply(mapStatus);
      } else {
        return Option.empty();
      }
    } finally {
      // free all memory & metadata, or memory leak happen in executor
      if (bufferManager != null) {
        bufferManager.freeAllMemory();
      }
      if (shuffleManager != null) {
        shuffleManager.clearTaskMeta(taskId);
      }
    }
  }

  @VisibleForTesting
  Map<Integer, Set<Long>> getPartitionToBlockIds() {
    return partitionToBlockIds;
  }
}
