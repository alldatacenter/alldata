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

package org.apache.uniffle.coordinator.strategy.storage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.filesystem.HadoopFilesystemProvider;
import org.apache.uniffle.coordinator.ApplicationManager;
import org.apache.uniffle.coordinator.CoordinatorConf;

/**
 * LowestIOSampleCostSelectStorageStrategy considers that when allocating apps to different remote paths,
 * remote paths that can write and read. Therefore, it may occur that all apps are written to the same cluster.
 * At the same time, if a cluster has read and write exceptions, we will automatically avoid the cluster.
 */
public class LowestIOSampleCostSelectStorageStrategy extends AbstractSelectStorageStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(LowestIOSampleCostSelectStorageStrategy.class);
  /**
   * store remote path -> application count for assignment strategy
   */
  private final Map<String, RemoteStorageInfo> appIdToRemoteStorageInfo;
  private final Map<String, RemoteStorageInfo> availableRemoteStorageInfo;
  private final Configuration hdfsConf;
  private final int readAndWriteTimes;
  private List<Map.Entry<String, RankValue>> uris;

  public LowestIOSampleCostSelectStorageStrategy(
      Map<String, RankValue> remoteStoragePathRankValue,
      Map<String, RemoteStorageInfo> appIdToRemoteStorageInfo,
      Map<String, RemoteStorageInfo> availableRemoteStorageInfo,
      CoordinatorConf conf) {
    super(remoteStoragePathRankValue, conf);
    this.appIdToRemoteStorageInfo = appIdToRemoteStorageInfo;
    this.availableRemoteStorageInfo = availableRemoteStorageInfo;
    this.hdfsConf = new Configuration();
    readAndWriteTimes = conf.getInteger(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_ACCESS_TIMES);
  }

  @VisibleForTesting
  public void sortPathByRankValue(
      String path, String testPath, long startWrite) {
    RankValue rankValue = remoteStoragePathRankValue.get(path);
    try {
      FileSystem fs = HadoopFilesystemProvider.getFilesystem(new Path(path), hdfsConf);
      fs.delete(new Path(testPath), true);
      if (rankValue.getHealthy().get()) {
        rankValue.setCostTime(new AtomicLong(System.currentTimeMillis() - startWrite));
      }
    } catch (Exception e) {
      rankValue.setCostTime(new AtomicLong(Long.MAX_VALUE));
      LOG.error("Failed to sort, we will not use this remote path {}.", path, e);
    }
    uris = Lists.newCopyOnWriteArrayList(
        remoteStoragePathRankValue.entrySet()).stream().filter(Objects::nonNull).sorted((x, y) -> {
          final long xReadAndWriteTime = x.getValue().getCostTime().get();
          final long yReadAndWriteTime = y.getValue().getCostTime().get();
          if (xReadAndWriteTime > yReadAndWriteTime) {
            return 1;
          } else if (xReadAndWriteTime < yReadAndWriteTime) {
            return -1;
          } else {
            return Integer.compare(x.getValue().getAppNum().get(), y.getValue().getAppNum().get());
          }
        }).collect(Collectors.toList());
    LOG.info("The sorted remote path list is: {}", uris);
  }

  @Override
  public void detectStorage() {
    uris = Lists.newCopyOnWriteArrayList(remoteStoragePathRankValue.entrySet());
    if (remoteStoragePathRankValue.size() > 1) {
      for (Map.Entry<String, RankValue> uri : uris) {
        if (uri.getKey().startsWith(ApplicationManager.getPathSchema().get(0))) {
          Path remotePath = new Path(uri.getKey());
          String rssTest = uri.getKey() + "/rssTest-" + getCoordinatorId();
          Path testPath = new Path(rssTest);
          RankValue rankValue = remoteStoragePathRankValue.get(uri.getKey());
          rankValue.setHealthy(new AtomicBoolean(true));
          long startWriteTime = System.currentTimeMillis();
          try {
            FileSystem fs = HadoopFilesystemProvider.getFilesystem(remotePath, hdfsConf);
            for (int j = 0; j < readAndWriteTimes; j++) {
              readAndWriteHdfsStorage(fs, testPath, uri.getKey(), rankValue);
            }
          } catch (Exception e) {
            LOG.error("Storage read and write error, we will not use this remote path {}.", uri, e);
            rankValue.setHealthy(new AtomicBoolean(false));
          } finally {
            sortPathByRankValue(uri.getKey(), rssTest, startWriteTime);
          }
        }
      }
    }
  }

  @Override
  public synchronized RemoteStorageInfo pickStorage(String appId) {
    LOG.info("The sorted remote path list is: {}", uris);
    for (Map.Entry<String, RankValue> uri : uris) {
      String storagePath = uri.getKey();
      if (availableRemoteStorageInfo.containsKey(storagePath)) {
        return appIdToRemoteStorageInfo.computeIfAbsent(appId, x -> availableRemoteStorageInfo.get(storagePath));
      }
    }
    LOG.warn("No remote storage is available, we will default to the first.");
    return availableRemoteStorageInfo.values().iterator().next();
  }
}
