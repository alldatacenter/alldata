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

import java.util.Comparator;
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
 * AppBalanceSelectStorageStrategy will consider the number of apps allocated on each remote path is balanced.
 */
public class AppBalanceSelectStorageStrategy extends AbstractSelectStorageStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(AppBalanceSelectStorageStrategy.class);
  /**
   * store remote path -> application count for assignment strategy
   */
  private final Map<String, RemoteStorageInfo> appIdToRemoteStorageInfo;
  private final Map<String, RemoteStorageInfo> availableRemoteStorageInfo;
  private final Configuration hdfsConf;
  private List<Map.Entry<String, RankValue>> uris;

  public AppBalanceSelectStorageStrategy(
      Map<String, RankValue> remoteStoragePathRankValue,
      Map<String, RemoteStorageInfo> appIdToRemoteStorageInfo,
      Map<String, RemoteStorageInfo> availableRemoteStorageInfo,
      CoordinatorConf conf) {
    super(remoteStoragePathRankValue, conf);
    this.appIdToRemoteStorageInfo = appIdToRemoteStorageInfo;
    this.availableRemoteStorageInfo = availableRemoteStorageInfo;
    this.hdfsConf = new Configuration();
  }

  @VisibleForTesting
  public void sortPathByRankValue(String path, String test) {
    RankValue rankValue = remoteStoragePathRankValue.get(path);
    try {
      FileSystem fs = HadoopFilesystemProvider.getFilesystem(new Path(path), hdfsConf);
      fs.delete(new Path(test),true);
      if (rankValue.getHealthy().get()) {
        rankValue.setCostTime(new AtomicLong(0));
      }
    } catch (Exception e) {
      rankValue.setCostTime(new AtomicLong(Long.MAX_VALUE));
      LOG.error("Failed to sort, we will not use this remote path {}.", path, e);
    }
    uris = Lists.newCopyOnWriteArrayList(remoteStoragePathRankValue.entrySet()).stream()
        .filter(Objects::nonNull).collect(Collectors.toList());
  }

  @Override
  public void detectStorage() {
    uris = Lists.newCopyOnWriteArrayList(remoteStoragePathRankValue.entrySet());
    if (remoteStoragePathRankValue.size() > 1) {
      for (Map.Entry<String, RankValue> uri : uris) {
        if (uri.getKey().startsWith(ApplicationManager.getPathSchema().get(0))) {
          RankValue rankValue = remoteStoragePathRankValue.get(uri.getKey());
          rankValue.setHealthy(new AtomicBoolean(true));
          Path remotePath = new Path(uri.getKey());
          String rssTest = uri.getKey() + "/rssTest-" + getCoordinatorId();
          Path testPath = new Path(rssTest);
          try {
            FileSystem fs = HadoopFilesystemProvider.getFilesystem(remotePath, hdfsConf);
            readAndWriteHdfsStorage(fs, testPath, uri.getKey(), rankValue);
          } catch (Exception e) {
            rankValue.setHealthy(new AtomicBoolean(false));
            LOG.error("Storage read and write error, we will not use this remote path {}.", uri, e);
          } finally {
            sortPathByRankValue(uri.getKey(), rssTest);
          }
        }
      }
    }
  }

  /**
   * When choosing the AppBalance strategy, each time you select a path,
   * you should know the number of the latest apps in different paths
   */
  @Override
  public synchronized RemoteStorageInfo pickStorage(String appId) {
    boolean isUnhealthy =
        uris.stream().noneMatch(rv -> rv.getValue().getCostTime().get() != Long.MAX_VALUE);
    if (!isUnhealthy) {
      // If there is only one unhealthy path, then filter that path
      uris = uris.stream().filter(rv -> rv.getValue().getCostTime().get() != Long.MAX_VALUE).sorted(
          Comparator.comparingInt(entry -> entry.getValue().getAppNum().get())).collect(Collectors.toList());
    } else {
      // If all paths are unhealthy, assign paths according to the number of apps
      uris = uris.stream().sorted(Comparator.comparingInt(
          entry -> entry.getValue().getAppNum().get())).collect(Collectors.toList());
    }
    LOG.info("The sorted remote path list is: {}", uris);
    for (Map.Entry<String, RankValue> entry : uris) {
      String storagePath = entry.getKey();
      if (availableRemoteStorageInfo.containsKey(storagePath)) {
        return appIdToRemoteStorageInfo.computeIfAbsent(appId, x -> availableRemoteStorageInfo.get(storagePath));
      }
    }
    LOG.warn("No remote storage is available, we will default to the first.");
    return availableRemoteStorageInfo.values().iterator().next();
  }
}
