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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.spark.*;
import org.apache.spark.launcher.SparkLauncher;
import org.apache.spark.shuffle.*;
import org.apache.spark.shuffle.sort.SortShuffleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.LifecycleManager;
import org.apache.celeborn.client.ShuffleClient;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.protocol.ShuffleMode;
import org.apache.celeborn.common.util.ThreadUtils;

public class RssShuffleManager implements ShuffleManager {

  private static final Logger logger = LoggerFactory.getLogger(RssShuffleManager.class);

  private static final String sortShuffleManagerName =
      "org.apache.spark.shuffle.sort.SortShuffleManager";

  private final SparkConf conf;
  private final CelebornConf celebornConf;
  private final int cores;
  private String newAppId;

  private LifecycleManager lifecycleManager;
  private ShuffleClient rssShuffleClient;
  private volatile SortShuffleManager _sortShuffleManager;
  private final ConcurrentHashMap.KeySetView<Integer, Boolean> sortShuffleIds =
      ConcurrentHashMap.newKeySet();
  private final RssShuffleFallbackPolicyRunner fallbackPolicyRunner;

  private final ExecutorService[] asyncPushers;
  private AtomicInteger pusherIdx = new AtomicInteger(0);

  public RssShuffleManager(SparkConf conf) {
    this.conf = conf;
    this.celebornConf = SparkUtils.fromSparkConf(conf);
    this.cores = conf.getInt(SparkLauncher.EXECUTOR_CORES, 1);
    this.fallbackPolicyRunner = new RssShuffleFallbackPolicyRunner(celebornConf);
    if (ShuffleMode.SORT.equals(celebornConf.shuffleWriterMode())
        && celebornConf.pushSortPipelineEnabled()) {
      asyncPushers = new ExecutorService[cores];
      for (int i = 0; i < asyncPushers.length; i++) {
        asyncPushers[i] = ThreadUtils.newDaemonSingleThreadExecutor("async-pusher-" + i);
      }
    } else {
      asyncPushers = null;
    }
  }

  private boolean isDriver() {
    return "driver".equals(SparkEnv.get().executorId());
  }

  private SortShuffleManager sortShuffleManager() {
    if (_sortShuffleManager == null) {
      synchronized (this) {
        if (_sortShuffleManager == null) {
          _sortShuffleManager =
              SparkUtils.instantiateClass(sortShuffleManagerName, conf, isDriver());
        }
      }
    }
    return _sortShuffleManager;
  }

  private void initializeLifecycleManager(String appId) {
    // Only create LifecycleManager singleton in Driver. When register shuffle multiple times, we
    // need to ensure that LifecycleManager will only be created once. Parallelism needs to be
    // considered in this place, because if there is one RDD that depends on multiple RDDs
    // at the same time, it may bring parallel `register shuffle`, such as Join in Sql.
    if (isDriver() && lifecycleManager == null) {
      synchronized (this) {
        if (lifecycleManager == null) {
          lifecycleManager = new LifecycleManager(appId, celebornConf);
          rssShuffleClient =
              ShuffleClient.get(
                  lifecycleManager.self(), celebornConf, lifecycleManager.getUserIdentifier());
        }
      }
    }
  }

  @Override
  public <K, V, C> ShuffleHandle registerShuffle(
      int shuffleId, ShuffleDependency<K, V, C> dependency) {
    // Note: generate newAppId at driver side, make sure dependency.rdd.context
    // is the same SparkContext among different shuffleIds.
    // This method may be called many times.
    newAppId = SparkUtils.genNewAppId(dependency.rdd().context());
    initializeLifecycleManager(newAppId);

    if (fallbackPolicyRunner.applyAllFallbackPolicy(
        lifecycleManager, dependency.partitioner().numPartitions())) {
      logger.warn("Fallback to SortShuffleManager!");
      sortShuffleIds.add(shuffleId);
      return sortShuffleManager().registerShuffle(shuffleId, dependency);
    } else {
      return new RssShuffleHandle<>(
          newAppId,
          lifecycleManager.getRssMetaServiceHost(),
          lifecycleManager.getRssMetaServicePort(),
          lifecycleManager.getUserIdentifier(),
          shuffleId,
          dependency.rdd().getNumPartitions(),
          dependency);
    }
  }

  @Override
  public boolean unregisterShuffle(int shuffleId) {
    if (sortShuffleIds.contains(shuffleId)) {
      return sortShuffleManager().unregisterShuffle(shuffleId);
    }
    if (newAppId == null) {
      return true;
    }
    if (rssShuffleClient == null) {
      return false;
    }
    return rssShuffleClient.unregisterShuffle(newAppId, shuffleId, isDriver());
  }

  @Override
  public ShuffleBlockResolver shuffleBlockResolver() {
    return sortShuffleManager().shuffleBlockResolver();
  }

  @Override
  public void stop() {
    if (rssShuffleClient != null) {
      rssShuffleClient.shutdown();
    }
    if (lifecycleManager != null) {
      lifecycleManager.stop();
    }
    if (sortShuffleManager() != null) {
      sortShuffleManager().stop();
    }
  }

  @Override
  public <K, V> ShuffleWriter<K, V> getWriter(
      ShuffleHandle handle, long mapId, TaskContext context, ShuffleWriteMetricsReporter metrics) {
    try {
      if (handle instanceof RssShuffleHandle) {
        @SuppressWarnings("unchecked")
        RssShuffleHandle<K, V, ?> h = ((RssShuffleHandle<K, V, ?>) handle);
        ShuffleClient client =
            ShuffleClient.get(
                h.rssMetaServiceHost(), h.rssMetaServicePort(), celebornConf, h.userIdentifier());
        if (ShuffleMode.SORT.equals(celebornConf.shuffleWriterMode())) {
          ExecutorService pushThread =
              celebornConf.pushSortPipelineEnabled() ? getPusherThread() : null;
          return new SortBasedShuffleWriter<>(
              h.dependency(),
              h.newAppId(),
              h.numMappers(),
              context,
              celebornConf,
              client,
              metrics,
              pushThread);
        } else if (ShuffleMode.HASH.equals(celebornConf.shuffleWriterMode())) {
          return new HashBasedShuffleWriter<>(
              h, context, celebornConf, client, metrics, SendBufferPool.get(cores));
        } else {
          throw new UnsupportedOperationException(
              "Unrecognized shuffle write mode!" + celebornConf.shuffleWriterMode());
        }
      } else {
        sortShuffleIds.add(handle.shuffleId());
        return sortShuffleManager().getWriter(handle, mapId, context, metrics);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // Added in SPARK-32055, for Spark 3.1 and above
  public <K, C> ShuffleReader<K, C> getReader(
      ShuffleHandle handle,
      int startMapIndex,
      int endMapIndex,
      int startPartition,
      int endPartition,
      TaskContext context,
      ShuffleReadMetricsReporter metrics) {
    if (handle instanceof RssShuffleHandle) {
      @SuppressWarnings("unchecked")
      RssShuffleHandle<K, ?, C> h = (RssShuffleHandle<K, ?, C>) handle;
      return new RssShuffleReader<>(
          h,
          startPartition,
          endPartition,
          startMapIndex,
          endMapIndex,
          context,
          celebornConf,
          metrics);
    }
    return SparkUtils.getReader(
        sortShuffleManager(),
        handle,
        startMapIndex,
        endMapIndex,
        startPartition,
        endPartition,
        context,
        metrics);
  }

  // Marked as final in SPARK-32055, reserved for Spark 3.0
  public <K, C> ShuffleReader<K, C> getReader(
      ShuffleHandle handle,
      int startPartition,
      int endPartition,
      TaskContext context,
      ShuffleReadMetricsReporter metrics) {
    if (handle instanceof RssShuffleHandle) {
      @SuppressWarnings("unchecked")
      RssShuffleHandle<K, ?, C> h = (RssShuffleHandle<K, ?, C>) handle;
      return new RssShuffleReader<>(
          h, startPartition, endPartition, 0, Integer.MAX_VALUE, context, celebornConf, metrics);
    }
    return SparkUtils.getReader(
        sortShuffleManager(),
        handle,
        0,
        Integer.MAX_VALUE,
        startPartition,
        endPartition,
        context,
        metrics);
  }

  // Renamed to getReader in SPARK-32055, reserved for Spark 3.0
  public <K, C> ShuffleReader<K, C> getReaderForRange(
      ShuffleHandle handle,
      int startMapIndex,
      int endMapIndex,
      int startPartition,
      int endPartition,
      TaskContext context,
      ShuffleReadMetricsReporter metrics) {
    if (handle instanceof RssShuffleHandle) {
      @SuppressWarnings("unchecked")
      RssShuffleHandle<K, ?, C> h = (RssShuffleHandle<K, ?, C>) handle;
      return new RssShuffleReader<>(
          h,
          startPartition,
          endPartition,
          startMapIndex,
          endMapIndex,
          context,
          celebornConf,
          metrics);
    }
    return SparkUtils.getReader(
        sortShuffleManager(),
        handle,
        startMapIndex,
        endMapIndex,
        startPartition,
        endPartition,
        context,
        metrics);
  }

  private ExecutorService getPusherThread() {
    ExecutorService pusherThread = asyncPushers[pusherIdx.get() % asyncPushers.length];
    pusherIdx.incrementAndGet();
    return pusherThread;
  }
}
