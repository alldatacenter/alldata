/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Utility singleton class to manage all the threads.
 */
public class ThreadPool {

  private static final Logger LOG = LoggerFactory.getLogger(ThreadPool.class);
  private static ThreadPool self;
  private static ScheduledExecutorService optimizeCheckPool;
  private static ScheduledExecutorService commitPool;
  private static ScheduledExecutorService expirePool;
  private static ScheduledExecutorService orphanPool;
  private static ScheduledExecutorService trashCleanPool;
  private static ScheduledExecutorService supportHiveSyncPool;
  private static ScheduledExecutorService optimizerMonitorPool;
  private static ThreadPoolExecutor syncFileInfoCachePool;
  private static ScheduledExecutorService tableRuntimeDataExpirePool;

  public enum Type {
    OPTIMIZE_CHECK,
    COMMIT,
    EXPIRE,
    ORPHAN,
    TRASH_CLEAN,
    SYNC_FILE_INFO_CACHE,
    OPTIMIZER_MONITOR,
    TABLE_RUNTIME_DATA_EXPIRE,
    HIVE_SYNC
  }

  public static synchronized ThreadPool initialize(Configuration conf) {
    if (self == null) {
      self = new ThreadPool(conf);
      LOG.debug("ThreadPool initialized");
    }
    return self;
  }

  private ThreadPool(Configuration conf) {
    ThreadFactory optimizeThreadFactory = new ThreadFactoryBuilder().setDaemon(false)
        .setNameFormat("Metastore Scheduled Check Worker %d").build();
    optimizeCheckPool =
        Executors.newScheduledThreadPool(
            conf.getInteger(ArcticMetaStoreConf.OPTIMIZE_CHECK_THREAD_POOL_SIZE),
            optimizeThreadFactory);

    ThreadFactory commitThreadFactory = new ThreadFactoryBuilder().setDaemon(false)
        .setNameFormat("Metastore Scheduled Commit Worker %d").build();
    commitPool = Executors.newScheduledThreadPool(
        conf.getInteger(ArcticMetaStoreConf.OPTIMIZE_COMMIT_THREAD_POOL_SIZE),
        commitThreadFactory);

    ThreadFactory expireThreadFactory = new ThreadFactoryBuilder().setDaemon(false)
        .setNameFormat("Metastore Scheduled Expire Worker %d").build();
    expirePool = Executors.newScheduledThreadPool(
        conf.getInteger(ArcticMetaStoreConf.EXPIRE_THREAD_POOL_SIZE),
        expireThreadFactory);

    ThreadFactory orphanThreadFactory = new ThreadFactoryBuilder().setDaemon(false)
        .setNameFormat("Metastore Scheduled Orphan Worker %d").build();
    orphanPool = Executors.newScheduledThreadPool(
        conf.getInteger(ArcticMetaStoreConf.ORPHAN_CLEAN_THREAD_POOL_SIZE),
        orphanThreadFactory);

    ThreadFactory trashThreadFactory = new ThreadFactoryBuilder().setDaemon(false)
        .setNameFormat("Metastore Scheduled Trash Worker %d").build();
    trashCleanPool = Executors.newScheduledThreadPool(
        conf.getInteger(ArcticMetaStoreConf.TRASH_CLEAN_THREAD_POOL_SIZE),
        trashThreadFactory);

    ThreadFactory supportHiveSyncFactory = new ThreadFactoryBuilder().setDaemon(false)
        .setNameFormat("Support Hive Sync Worker %d").build();
    supportHiveSyncPool = Executors.newScheduledThreadPool(
        conf.getInteger(ArcticMetaStoreConf.SUPPORT_HIVE_SYNC_THREAD_POOL_SIZE),
        supportHiveSyncFactory);

    ThreadFactory optimizerMonitorThreadFactory = new ThreadFactoryBuilder().setDaemon(false)
        .setNameFormat("Metastore Scheduled Optimizer Monitor Worker %d").build();
    optimizerMonitorPool = Executors.newScheduledThreadPool(
        1,
        optimizerMonitorThreadFactory);

    ThreadFactory syncFileInfoCachePoolThreadFactory = new ThreadFactoryBuilder().setDaemon(false)
        .setNameFormat("Metastore Scheduled SyncFileInfoCache Monitor Worker %d").build();
    syncFileInfoCachePool =
        new ThreadPoolExecutor(
            conf.getInteger(ArcticMetaStoreConf.SYNC_FILE_INFO_CACHE_THREAD_POOL_SIZE),
            conf.getInteger(ArcticMetaStoreConf.SYNC_FILE_INFO_CACHE_THREAD_POOL_SIZE),
            1,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(),
            syncFileInfoCachePoolThreadFactory);

    ThreadFactory tableRuntimeDataExpirePoolThreadFactory = new ThreadFactoryBuilder().setDaemon(false)
        .setNameFormat("Metastore Scheduled Table Runtime Data Expire Worker %d").build();
    tableRuntimeDataExpirePool = Executors.newScheduledThreadPool(
        1,
        tableRuntimeDataExpirePoolThreadFactory);
  }

  public static ScheduledExecutorService getPool(Type type) {
    if (self == null) {
      throw new RuntimeException("ThreadPool accessed before initialized");
    }
    switch (type) {
      case OPTIMIZE_CHECK:
        return optimizeCheckPool;
      case COMMIT:
        return commitPool;
      case EXPIRE:
        return expirePool;
      case ORPHAN:
        return orphanPool;
      case TRASH_CLEAN:
        return trashCleanPool;
      case OPTIMIZER_MONITOR:
        return optimizerMonitorPool;
      case TABLE_RUNTIME_DATA_EXPIRE:
        return tableRuntimeDataExpirePool;
      case HIVE_SYNC:
        return supportHiveSyncPool;
      default:
        throw new RuntimeException("ThreadPool not support this type: " + type);
    }
  }

  public static synchronized void shutdown() {
    if (self != null) {
      optimizeCheckPool.shutdownNow();
      optimizerMonitorPool.shutdownNow();
      commitPool.shutdownNow();
      expirePool.shutdownNow();
      orphanPool.shutdownNow();
      trashCleanPool.shutdownNow();
      syncFileInfoCachePool.shutdownNow();
      tableRuntimeDataExpirePool.shutdownNow();
      supportHiveSyncPool.shutdownNow();
      self = null;
    }
  }
}
