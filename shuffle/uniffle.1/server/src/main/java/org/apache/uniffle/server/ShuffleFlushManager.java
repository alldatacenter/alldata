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

package org.apache.uniffle.server;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.common.config.RssBaseConf;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.common.util.ThreadUtils;
import org.apache.uniffle.server.storage.MultiStorageManager;
import org.apache.uniffle.server.storage.StorageManager;
import org.apache.uniffle.storage.common.Storage;
import org.apache.uniffle.storage.handler.api.ShuffleWriteHandler;
import org.apache.uniffle.storage.request.CreateShuffleWriteHandlerRequest;

public class ShuffleFlushManager {

  private static final Logger LOG = LoggerFactory.getLogger(ShuffleFlushManager.class);
  public static final AtomicLong ATOMIC_EVENT_ID = new AtomicLong(0);
  private final ShuffleServer shuffleServer;
  protected final BlockingQueue<ShuffleDataFlushEvent> flushQueue = Queues.newLinkedBlockingQueue();
  private final Executor threadPoolExecutor;
  private final List<String> storageBasePaths;
  private final String shuffleServerId;
  private final String storageType;
  private final int storageDataReplica;
  private final ShuffleServerConf shuffleServerConf;
  private Configuration hadoopConf;
  // appId -> shuffleId -> committed shuffle blockIds
  private Map<String, Map<Integer, Roaring64NavigableMap>> committedBlockIds = Maps.newConcurrentMap();
  private final int retryMax;

  private final StorageManager storageManager;
  private final BlockingQueue<PendingShuffleFlushEvent> pendingEvents = Queues.newLinkedBlockingQueue();
  private final long pendingEventTimeoutSec;
  private int processPendingEventIndex = 0;
  private final int maxConcurrencyOfSingleOnePartition;

  public ShuffleFlushManager(ShuffleServerConf shuffleServerConf, String shuffleServerId, ShuffleServer shuffleServer,
                             StorageManager storageManager) {
    this.shuffleServerId = shuffleServerId;
    this.shuffleServer = shuffleServer;
    this.shuffleServerConf = shuffleServerConf;
    this.storageManager = storageManager;
    initHadoopConf();
    retryMax = shuffleServerConf.getInteger(ShuffleServerConf.SERVER_WRITE_RETRY_MAX);
    storageType = shuffleServerConf.get(RssBaseConf.RSS_STORAGE_TYPE);
    storageDataReplica = shuffleServerConf.get(RssBaseConf.RSS_STORAGE_DATA_REPLICA);
    this.maxConcurrencyOfSingleOnePartition =
        shuffleServerConf.get(ShuffleServerConf.SERVER_MAX_CONCURRENCY_OF_ONE_PARTITION);

    storageBasePaths = RssUtils.getConfiguredLocalDirs(shuffleServerConf);
    pendingEventTimeoutSec = shuffleServerConf.getLong(ShuffleServerConf.PENDING_EVENT_TIMEOUT_SEC);
    threadPoolExecutor = createFlushEventExecutor();
    startEventProcessor();
    // todo: extract a class named Service, and support stop method
    Thread thread = new Thread("PendingEventProcessThread") {
      @Override
      public void run() {
        for (; ; ) {
          try {
            processPendingEvents();
            processPendingEventIndex = (processPendingEventIndex + 1) % 1000;
            if (processPendingEventIndex == 0) {
              // todo: get sleep interval from configuration
              Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            }
          } catch (Exception e) {
            LOG.error(getName() + " happened exception: ", e);
          }
        }
      }
    };
    thread.setDaemon(true);
    thread.start();
  }

  private void startEventProcessor() {
    // the thread for flush data
    Thread processEventThread = new Thread(this::eventLoop);
    processEventThread.setName("ProcessEventThread");
    processEventThread.setDaemon(true);
    processEventThread.start();
  }

  protected Executor createFlushEventExecutor() {
    int waitQueueSize = shuffleServerConf.getInteger(
        ShuffleServerConf.SERVER_FLUSH_THREAD_POOL_QUEUE_SIZE);
    BlockingQueue<Runnable> waitQueue = Queues.newLinkedBlockingQueue(waitQueueSize);
    int poolSize = shuffleServerConf.getInteger(ShuffleServerConf.SERVER_FLUSH_THREAD_POOL_SIZE);
    long keepAliveTime = shuffleServerConf.getLong(ShuffleServerConf.SERVER_FLUSH_THREAD_ALIVE);
    return new ThreadPoolExecutor(poolSize, poolSize, keepAliveTime, TimeUnit.SECONDS, waitQueue,
        ThreadUtils.getThreadFactory("FlushEventThreadPool"));
  }

  public void addToFlushQueue(ShuffleDataFlushEvent event) {
    if (!flushQueue.offer(event)) {
      LOG.warn("Flush queue is full, discard event: " + event);
    } else {
      ShuffleServerMetrics.gaugeEventQueueSize.inc();
    }
  }

  protected void eventLoop() {
    while (true) {
      processNextEvent();
    }
  }

  protected void processNextEvent() {
    try {
      ShuffleDataFlushEvent event = flushQueue.take();
      threadPoolExecutor.execute(() -> processEvent(event));
    } catch (Exception e) {
      LOG.error("Exception happened when process event.", e);
    }
  }

  private void processEvent(ShuffleDataFlushEvent event) {
    try {
      ShuffleServerMetrics.gaugeWriteHandler.inc();
      flushToFile(event);
    } catch (Exception e) {
      LOG.error("Exception happened when flush data for " + event, e);
    } finally {
      ShuffleServerMetrics.gaugeWriteHandler.dec();
      ShuffleServerMetrics.gaugeEventQueueSize.dec();
    }
  }

  private void flushToFile(ShuffleDataFlushEvent event) {
    long start = System.currentTimeMillis();
    boolean writeSuccess = false;

    while (event.getRetryTimes() <= retryMax) {
      try {
        if (!event.isValid()) {
          writeSuccess = true;
          LOG.warn("AppId {} was removed already, event {} should be dropped", event.getAppId(), event);
          break;
        }

        List<ShufflePartitionedBlock> blocks = event.getShuffleBlocks();
        if (blocks == null || blocks.isEmpty()) {
          LOG.info("There is no block to be flushed: {}", event);
          break;
        }

        Storage storage = storageManager.selectStorage(event);
        if (storage == null) {
          LOG.error("Storage selected is null and this should not happen. event: {}", event);
          break;
        }

        if (!storage.canWrite()) {
          // todo: Could we add an interface supportPending for storageManager
          //       to unify following logic of multiple different storage managers
          if (storageManager instanceof MultiStorageManager) {
            event.increaseRetryTimes();
            ShuffleServerMetrics.incStorageRetryCounter(storage.getStorageHost());
            continue;
          } else {
            // To avoid being re-pushed to pending queue and make the server too much pressure,
            // it's better to drop directly.
            if (event.isPended()) {
              LOG.error("Drop this event directly due to already having entered pending queue. event: {}", event);
              break;
            }
            event.increaseRetryTimes();
            event.markPended();
            addPendingEvents(event);
            return;
          }
        }

        String user = StringUtils.defaultString(
            shuffleServer.getShuffleTaskManager().getUserByAppId(event.getAppId()),
            StringUtils.EMPTY
        );
        CreateShuffleWriteHandlerRequest request = new CreateShuffleWriteHandlerRequest(
            storageType,
            event.getAppId(),
            event.getShuffleId(),
            event.getStartPartition(),
            event.getEndPartition(),
            storageBasePaths.toArray(new String[storageBasePaths.size()]),
            shuffleServerId,
            hadoopConf,
            storageDataReplica,
            user,
            maxConcurrencyOfSingleOnePartition);
        ShuffleWriteHandler handler = storage.getOrCreateWriteHandler(request);
        writeSuccess = storageManager.write(storage, handler, event);
        if (writeSuccess) {
          updateCommittedBlockIds(event.getAppId(), event.getShuffleId(), blocks);
          ShuffleServerMetrics.incStorageSuccessCounter(storage.getStorageHost());
          break;
        } else {
          event.increaseRetryTimes();
          ShuffleServerMetrics.incStorageRetryCounter(storage.getStorageHost());
        }
      } catch (Throwable throwable) {
        // just log the error, don't throw the exception and stop the flush thread
        LOG.error("Exception happened when process flush shuffle data for {}", event, throwable);
        event.increaseRetryTimes();
      }
    }

    if (event.getRetryTimes() > retryMax) {
      LOG.error("Failed to write data for {} in {} times, shuffle data will be lost", event, retryMax);
      if (event.getUnderStorage() != null) {
        ShuffleServerMetrics.incStorageFailedCounter(event.getUnderStorage().getStorageHost());
      }
    }

    event.doCleanup();
    if (shuffleServer != null) {
      long duration = System.currentTimeMillis() - start;
      if (writeSuccess) {
        LOG.debug("Flush to file success in {} ms and release {} bytes", duration, event.getSize());
      } else {
        ShuffleServerMetrics.counterTotalFailedWrittenEventNum.inc();
        LOG.error("Flush to file for {} failed in {} ms and release {} bytes", event, duration, event.getSize());
      }
    }
  }
  
  private void updateCommittedBlockIds(String appId, int shuffleId, List<ShufflePartitionedBlock> blocks) {
    if (blocks == null || blocks.size() == 0) {
      return;
    }
    if (!committedBlockIds.containsKey(appId)) {
      committedBlockIds.putIfAbsent(appId, Maps.newConcurrentMap());
    }
    Map<Integer, Roaring64NavigableMap> shuffleToBlockIds = committedBlockIds.get(appId);
    shuffleToBlockIds.putIfAbsent(shuffleId, Roaring64NavigableMap.bitmapOf());
    Roaring64NavigableMap bitmap = shuffleToBlockIds.get(shuffleId);
    synchronized (bitmap) {
      for (ShufflePartitionedBlock spb : blocks) {
        bitmap.addLong(spb.getBlockId());
      }
    }
  }

  public Roaring64NavigableMap getCommittedBlockIds(String appId, Integer shuffleId) {
    Map<Integer, Roaring64NavigableMap> shuffleIdToBlockIds = committedBlockIds.get(appId);
    if (shuffleIdToBlockIds == null) {
      LOG.warn("Unexpected value when getCommittedBlockIds for appId[" + appId + "]");
      return Roaring64NavigableMap.bitmapOf();
    }
    Roaring64NavigableMap blockIds = shuffleIdToBlockIds.get(shuffleId);
    if (blockIds == null) {
      LOG.warn("Unexpected value when getCommittedBlockIds for appId[" + appId + "], shuffleId[" + shuffleId + "]");
      return Roaring64NavigableMap.bitmapOf();
    }
    return blockIds;
  }

  public void removeResources(String appId) {
    committedBlockIds.remove(appId);
  }

  protected void initHadoopConf() {
    hadoopConf = new Configuration();
    for (String key : shuffleServerConf.getKeySet()) {
      if (key.startsWith(ShuffleServerConf.PREFIX_HADOOP_CONF)) {
        String value = shuffleServerConf.getString(key, "");
        String hadoopKey = key.substring(ShuffleServerConf.PREFIX_HADOOP_CONF.length() + 1);
        LOG.info("Update hadoop configuration:" + hadoopKey + "=" + value);
        hadoopConf.set(hadoopKey, value);
      }
    }
  }

  public int getEventNumInFlush() {
    return flushQueue.size();
  }

  public Configuration getHadoopConf() {
    return hadoopConf;
  }

  @VisibleForTesting
  void processPendingEvents() throws Exception {
    PendingShuffleFlushEvent event = pendingEvents.take();
    Storage storage = storageManager.selectStorage(event.getEvent());
    if (storage == null) {
      dropPendingEvent(event);
      LOG.error("Flush event cannot be flushed because of application related was cleared, {}", event.getEvent());
      return;
    }
    if (System.currentTimeMillis() - event.getCreateTimeStamp() > pendingEventTimeoutSec * 1000L) {
      dropPendingEvent(event);
      LOG.error("Flush event cannot be flushed for {} sec, the event {} is dropped",
          pendingEventTimeoutSec, event.getEvent());
      return;
    }
    // storage maybe null if the application cache was cleared already
    // add event to flush queue, and it will be released
    if (storage.canWrite()) {
      addToFlushQueue(event.getEvent());
      return;
    }
    addPendingEventsInternal(event);
  }

  private void dropPendingEvent(PendingShuffleFlushEvent event) {
    ShuffleServerMetrics.counterTotalDroppedEventNum.inc();
    event.getEvent().doCleanup();
  }

  @VisibleForTesting
  void addPendingEvents(ShuffleDataFlushEvent event) {
    addPendingEventsInternal(new PendingShuffleFlushEvent(event));
  }

  @VisibleForTesting
  int getPendingEventsSize() {
    return pendingEvents.size();
  }

  private void addPendingEventsInternal(PendingShuffleFlushEvent event) {
    boolean pendingEventsResult = pendingEvents.offer(event);
    ShuffleDataFlushEvent flushEvent = event.getEvent();
    if (!pendingEventsResult) {
      LOG.error("Post pendingEvent queue fail!! App: " + flushEvent.getAppId() + " Shuffle "
          + flushEvent.getShuffleId() + " Partition " + flushEvent.getStartPartition());
    }
  }

  public void removeResourcesOfShuffleId(String appId, Collection<Integer> shuffleIds) {
    Optional.ofNullable(committedBlockIds.get(appId))
        .ifPresent(shuffleIdToBlockIds -> shuffleIds.forEach(shuffleIdToBlockIds::remove));
  }

  private static class PendingShuffleFlushEvent {
    private final ShuffleDataFlushEvent event;
    private final long createTimeStamp = System.currentTimeMillis();

    PendingShuffleFlushEvent(ShuffleDataFlushEvent event) {
      this.event = event;
    }

    public ShuffleDataFlushEvent getEvent() {
      return event;
    }

    public long getCreateTimeStamp() {
      return createTimeStamp;
    }
  }

  public ShuffleDataDistributionType getDataDistributionType(String appId) {
    return shuffleServer.getShuffleTaskManager().getDataDistributionType(appId);
  }
}
