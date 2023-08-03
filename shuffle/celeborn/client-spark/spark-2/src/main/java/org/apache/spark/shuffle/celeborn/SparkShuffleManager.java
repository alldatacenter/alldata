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

import scala.Int;

import org.apache.spark.*;
import org.apache.spark.launcher.SparkLauncher;
import org.apache.spark.shuffle.*;
import org.apache.spark.shuffle.sort.SortShuffleManager;
import org.apache.spark.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.LifecycleManager;
import org.apache.celeborn.client.ShuffleClient;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.protocol.ShuffleMode;
import org.apache.celeborn.common.util.ThreadUtils;
import org.apache.celeborn.reflect.DynMethods;

public class SparkShuffleManager implements ShuffleManager {

  private static final Logger logger = LoggerFactory.getLogger(SparkShuffleManager.class);

  private static final String sortShuffleManagerName =
      "org.apache.spark.shuffle.sort.SortShuffleManager";

  private final SparkConf conf;
  private final CelebornConf celebornConf;
  private final int cores;
  // either be "{appId}_{appAttemptId}" or "{appId}"
  private String appUniqueId;

  private LifecycleManager lifecycleManager;
  private ShuffleClient shuffleClient;
  private volatile SortShuffleManager _sortShuffleManager;
  private final ConcurrentHashMap.KeySetView<Integer, Boolean> sortShuffleIds =
      ConcurrentHashMap.newKeySet();
  private final CelebornShuffleFallbackPolicyRunner fallbackPolicyRunner;

  private final ExecutorService[] asyncPushers;
  private AtomicInteger pusherIdx = new AtomicInteger(0);

  public SparkShuffleManager(SparkConf conf) {
    this.conf = conf;
    this.celebornConf = SparkUtils.fromSparkConf(conf);
    this.cores = conf.getInt(SparkLauncher.EXECUTOR_CORES, 1);
    this.fallbackPolicyRunner = new CelebornShuffleFallbackPolicyRunner(celebornConf);
    if (ShuffleMode.SORT.equals(celebornConf.shuffleWriterMode())
        && celebornConf.clientPushSortPipelineEnabled()) {
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
          shuffleClient =
              ShuffleClient.get(
                  appUniqueId,
                  lifecycleManager.getHost(),
                  lifecycleManager.getPort(),
                  celebornConf,
                  lifecycleManager.getUserIdentifier(),
                  true);
        }
      }
    }
  }

  @Override
  public <K, V, C> ShuffleHandle registerShuffle(
      int shuffleId, int numMaps, ShuffleDependency<K, V, C> dependency) {
    // Note: generate app unique id at driver side, make sure dependency.rdd.context
    // is the same SparkContext among different shuffleIds.
    // This method may be called many times.
    appUniqueId = SparkUtils.appUniqueId(dependency.rdd().context());
    initializeLifecycleManager(appUniqueId);

    if (fallbackPolicyRunner.applyAllFallbackPolicy(
        lifecycleManager, dependency.partitioner().numPartitions())) {
      logger.warn("Fallback to SortShuffleManager!");
      sortShuffleIds.add(shuffleId);
      return sortShuffleManager().registerShuffle(shuffleId, numMaps, dependency);
    } else {
      return new CelebornShuffleHandle<>(
          appUniqueId,
          lifecycleManager.getHost(),
          lifecycleManager.getPort(),
          lifecycleManager.getUserIdentifier(),
          shuffleId,
          numMaps,
          dependency);
    }
  }

  @Override
  public boolean unregisterShuffle(int shuffleId) {
    if (sortShuffleIds.contains(shuffleId)) {
      return sortShuffleManager().unregisterShuffle(shuffleId);
    }
    if (appUniqueId == null) {
      return true;
    }
    if (shuffleClient == null) {
      return false;
    }
    return shuffleClient.unregisterShuffle(shuffleId, isDriver());
  }

  @Override
  public ShuffleBlockResolver shuffleBlockResolver() {
    return sortShuffleManager().shuffleBlockResolver();
  }

  @Override
  public void stop() {
    if (shuffleClient != null) {
      shuffleClient.shutdown();
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
      ShuffleHandle handle, int mapId, TaskContext context) {
    try {
      if (handle instanceof CelebornShuffleHandle) {
        @SuppressWarnings("unchecked")
        CelebornShuffleHandle<K, V, ?> h = ((CelebornShuffleHandle<K, V, ?>) handle);
        ShuffleClient client =
            ShuffleClient.get(
                h.appUniqueId(),
                h.lifecycleManagerHost(),
                h.lifecycleManagerPort(),
                celebornConf,
                h.userIdentifier(),
                false);
        if (ShuffleMode.SORT.equals(celebornConf.shuffleWriterMode())) {
          ExecutorService pushThread =
              celebornConf.clientPushSortPipelineEnabled() ? getPusherThread() : null;
          return new SortBasedShuffleWriter<>(
              h.dependency(),
              h.numMaps(),
              context,
              celebornConf,
              client,
              pushThread,
              SendBufferPool.get(cores));
        } else if (ShuffleMode.HASH.equals(celebornConf.shuffleWriterMode())) {
          return new HashBasedShuffleWriter<>(
              h, mapId, context, celebornConf, client, SendBufferPool.get(cores));
        } else {
          throw new UnsupportedOperationException(
              "Unrecognized shuffle write mode!" + celebornConf.shuffleWriterMode());
        }
      } else {
        sortShuffleIds.add(handle.shuffleId());
        return sortShuffleManager().getWriter(handle, mapId, context);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public <K, C> ShuffleReader<K, C> getReader(
      ShuffleHandle handle, int startPartition, int endPartition, TaskContext context) {
    if (handle instanceof CelebornShuffleHandle) {
      @SuppressWarnings("unchecked")
      CelebornShuffleHandle<K, ?, C> h = (CelebornShuffleHandle<K, ?, C>) handle;
      return new CelebornShuffleReader<>(
          h, startPartition, endPartition, 0, Int.MaxValue(), context, celebornConf);
    }
    return _sortShuffleManager.getReader(handle, startPartition, endPartition, context);
  }

  private ExecutorService getPusherThread() {
    ExecutorService pusherThread = asyncPushers[pusherIdx.get() % asyncPushers.length];
    pusherIdx.incrementAndGet();
    return pusherThread;
  }

  private int executorCores(SparkConf conf) {
    if (Utils.isLocalMaster(conf)) {
      // SparkContext.numDriverCores is package private.
      return DynMethods.builder("numDriverCores")
          .impl("org.apache.spark.SparkContext$", String.class)
          .build()
          .bind(SparkContext$.MODULE$)
          .invoke(conf.get("spark.master"));
    } else {
      return conf.getInt(SparkLauncher.EXECUTOR_CORES, 1);
    }
  }

  // for testing
  public LifecycleManager getLifecycleManager() {
    return this.lifecycleManager;
  }
}
