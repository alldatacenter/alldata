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

package org.apache.uniffle.coordinator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.common.util.ThreadUtils;
import org.apache.uniffle.coordinator.access.checker.AccessQuotaChecker;
import org.apache.uniffle.coordinator.metric.CoordinatorMetrics;
import org.apache.uniffle.coordinator.strategy.storage.AppBalanceSelectStorageStrategy;
import org.apache.uniffle.coordinator.strategy.storage.LowestIOSampleCostSelectStorageStrategy;
import org.apache.uniffle.coordinator.strategy.storage.RankValue;
import org.apache.uniffle.coordinator.strategy.storage.SelectStorageStrategy;
import org.apache.uniffle.coordinator.util.CoordinatorUtils;

public class ApplicationManager {

  private static final Logger LOG = LoggerFactory.getLogger(ApplicationManager.class);
  // TODO: Add anomaly detection for other storage
  private static final List<String> REMOTE_PATH_SCHEMA = Arrays.asList("hdfs");
  private final long expired;
  private final StrategyName storageStrategy;
  private final SelectStorageStrategy selectStorageStrategy;
  // store appId -> remote path to make sure all shuffle data of the same application
  // will be written to the same remote storage
  private final Map<String, RemoteStorageInfo> appIdToRemoteStorageInfo;
  // store remote path -> application count for assignment strategy
  private final Map<String, RankValue> remoteStoragePathRankValue;
  private final Map<String, String> remoteStorageToHost = Maps.newConcurrentMap();
  private final Map<String, RemoteStorageInfo> availableRemoteStorageInfo;
  private final ScheduledExecutorService detectStorageScheduler;
  private Map<String, Map<String, Long>> currentUserAndApp = Maps.newConcurrentMap();
  private Map<String, String> appIdToUser = Maps.newConcurrentMap();
  private QuotaManager quotaManager;
  // it's only for test case to check if status check has problem
  private boolean hasErrorInStatusCheck = false;

  public ApplicationManager(CoordinatorConf conf) {
    storageStrategy = conf.get(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SELECT_STRATEGY);
    appIdToRemoteStorageInfo = Maps.newConcurrentMap();
    remoteStoragePathRankValue = Maps.newConcurrentMap();
    availableRemoteStorageInfo = Maps.newConcurrentMap();
    if (StrategyName.IO_SAMPLE == storageStrategy) {
      selectStorageStrategy = new LowestIOSampleCostSelectStorageStrategy(remoteStoragePathRankValue,
          appIdToRemoteStorageInfo, availableRemoteStorageInfo, conf);
    } else if (StrategyName.APP_BALANCE == storageStrategy) {
      selectStorageStrategy = new AppBalanceSelectStorageStrategy(remoteStoragePathRankValue,
          appIdToRemoteStorageInfo, availableRemoteStorageInfo, conf);
    } else {
      throw new UnsupportedOperationException("Unsupported selected storage strategy.");
    }
    expired = conf.getLong(CoordinatorConf.COORDINATOR_APP_EXPIRED);
    String quotaCheckerClass = AccessQuotaChecker.class.getCanonicalName();
    for (String checker : conf.get(CoordinatorConf.COORDINATOR_ACCESS_CHECKERS)) {
      if (quotaCheckerClass.equals(checker.trim())) {
        this.quotaManager = new QuotaManager(conf);
        this.currentUserAndApp = quotaManager.getCurrentUserAndApp();
        this.appIdToUser = quotaManager.getAppIdToUser();
        break;
      }
    }
    // the thread for checking application status
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ThreadUtils.getThreadFactory("ApplicationManager-%d"));
    scheduledExecutorService.scheduleAtFixedRate(
        this::statusCheck, expired / 2, expired / 2, TimeUnit.MILLISECONDS);
    // the thread for checking if the storage is normal
    detectStorageScheduler = Executors.newSingleThreadScheduledExecutor(
        ThreadUtils.getThreadFactory("detectStoragesScheduler-%d"));
    // should init later than the refreshRemoteStorage init
    detectStorageScheduler.scheduleAtFixedRate(selectStorageStrategy::detectStorage, 1000,
        conf.getLong(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_TIME), TimeUnit.MILLISECONDS);
  }

  public void registerApplicationInfo(String appId, String user) {
    // using computeIfAbsent is just for MR and spark which is used RssShuffleManager as implementation class
    // in such case by default, there is no currentUserAndApp, so a unified user implementation named "user" is used.
    Map<String, Long> appAndTime = currentUserAndApp.computeIfAbsent(user, x -> Maps.newConcurrentMap());
    appIdToUser.put(appId, user);
    if (!appAndTime.containsKey(appId)) {
      CoordinatorMetrics.counterTotalAppNum.inc();
      LOG.info("New application is registered: {}", appId);
    }
    if (quotaManager != null) {
      quotaManager.registerApplicationInfo(appId, appAndTime);
    } else {
      appAndTime.put(appId, System.currentTimeMillis());
    }
  }

  public void refreshAppId(String appId) {
    String user = appIdToUser.get(appId);
    // compatible with lower version clients
    if (user == null) {
      registerApplicationInfo(appId, "");
    } else {
      Map<String, Long> appAndTime = currentUserAndApp.get(user);
      appAndTime.put(appId, System.currentTimeMillis());
    }
  }

  public void refreshRemoteStorage(String remoteStoragePath, String remoteStorageConf) {
    if (!StringUtils.isEmpty(remoteStoragePath)) {
      LOG.info("Refresh remote storage with {} {}", remoteStoragePath, remoteStorageConf);
      Set<String> paths = Sets.newHashSet(remoteStoragePath.split(Constants.COMMA_SPLIT_CHAR));
      Map<String, Map<String, String>> confKVs = CoordinatorUtils.extractRemoteStorageConf(remoteStorageConf);
      // add remote path if not exist
      for (String path : paths) {
        if (!availableRemoteStorageInfo.containsKey(path)) {
          remoteStoragePathRankValue.putIfAbsent(path, new RankValue(0));
          // refreshRemoteStorage is designed without multiple thread problem
          // metrics shouldn't be added duplicated
          addRemoteStorageMetrics(path);
        }
        String storageHost = getStorageHost(path);
        RemoteStorageInfo rsInfo = new RemoteStorageInfo(path, confKVs.getOrDefault(storageHost, Maps.newHashMap()));
        availableRemoteStorageInfo.put(path, rsInfo);
      }
      // remove unused remote path if exist
      List<String> unusedPath = Lists.newArrayList();
      for (String existPath : availableRemoteStorageInfo.keySet()) {
        if (!paths.contains(existPath)) {
          unusedPath.add(existPath);
        }
      }
      // remote unused path
      for (String path : unusedPath) {
        availableRemoteStorageInfo.remove(path);
        // try to remove if counter = 0, or it will be removed in decRemoteStorageCounter() later
        removePathFromCounter(path);
      }
    } else {
      LOG.info("Refresh remote storage with empty value {}", remoteStoragePath);
      for (String path : availableRemoteStorageInfo.keySet()) {
        removePathFromCounter(path);
      }
      availableRemoteStorageInfo.clear();
    }
  }

  // the strategy of pick remote storage is according to assignment count
  // todo: better strategy with workload balance
  public RemoteStorageInfo pickRemoteStorage(String appId) {
    if (appIdToRemoteStorageInfo.containsKey(appId)) {
      return appIdToRemoteStorageInfo.get(appId);
    }
    RemoteStorageInfo pickStorage = selectStorageStrategy.pickStorage(appId);
    incRemoteStorageCounter(pickStorage.getPath());
    return appIdToRemoteStorageInfo.get(appId);
  }

  @VisibleForTesting
  public synchronized void incRemoteStorageCounter(String remoteStoragePath) {
    RankValue counter = remoteStoragePathRankValue.get(remoteStoragePath);
    if (counter != null) {
      counter.getAppNum().incrementAndGet();
    } else {
      // it may be happened when assignment remote storage
      // and refresh remote storage at the same time
      LOG.warn("Remote storage path lost during assignment: {} doesn't exist, reset it to 1",
          remoteStoragePath);
      remoteStoragePathRankValue.put(remoteStoragePath, new RankValue(1));
    }
  }

  @VisibleForTesting
  public synchronized void decRemoteStorageCounter(String storagePath) {
    if (!StringUtils.isEmpty(storagePath)) {
      RankValue atomic = remoteStoragePathRankValue.get(storagePath);
      if (atomic != null) {
        double count = atomic.getAppNum().decrementAndGet();
        if (count < 0) {
          LOG.warn("Unexpected counter for remote storage: {}, which is {}, reset to 0",
              storagePath, count);
          atomic.getAppNum().set(0);
        }
      } else {
        LOG.warn("Can't find counter for remote storage: {}", storagePath);
        remoteStoragePathRankValue.putIfAbsent(storagePath, new RankValue(0));
      }
      if (remoteStoragePathRankValue.get(storagePath).getAppNum().get() == 0
          && !availableRemoteStorageInfo.containsKey(storagePath)) {
        remoteStoragePathRankValue.remove(storagePath);
      }
    }
  }

  public synchronized void removePathFromCounter(String storagePath) {
    RankValue atomic = remoteStoragePathRankValue.get(storagePath);
    // The time spent reading and writing cannot be used to determine whether the current path is still used by apps.
    // Therefore, determine whether the HDFS path is still used by the number of apps
    if (atomic != null && atomic.getAppNum().get() == 0) {
      remoteStoragePathRankValue.remove(storagePath);
    }
  }

  public Set<String> getAppIds() {
    return appIdToUser.keySet();
  }

  @VisibleForTesting
  public Map<String, RankValue> getRemoteStoragePathRankValue() {
    return remoteStoragePathRankValue;
  }

  @VisibleForTesting
  public SelectStorageStrategy getSelectStorageStrategy() {
    return selectStorageStrategy;
  }

  @VisibleForTesting
  public Map<String, RemoteStorageInfo> getAvailableRemoteStorageInfo() {
    return availableRemoteStorageInfo;
  }

  @VisibleForTesting
  public Map<String, RemoteStorageInfo> getAppIdToRemoteStorageInfo() {
    return appIdToRemoteStorageInfo;
  }

  @VisibleForTesting
  public boolean hasErrorInStatusCheck() {
    return hasErrorInStatusCheck;
  }

  @VisibleForTesting
  public void closeDetectStorageScheduler() {
    // this method can only be used during testing
    detectStorageScheduler.shutdownNow();
  }

  private void statusCheck() {
    List<Map<String, Long>> appAndNums = Lists.newArrayList(currentUserAndApp.values());
    Map<String, Long> appIds = Maps.newHashMap();
    // The reason for setting an expired uuid here is that there is a scenario where accessCluster succeeds,
    // but the registration of shuffle fails, resulting in no normal heartbeat, and no normal update of uuid to appId.
    // Therefore, an expiration time is set to automatically remove expired uuids
    Set<String> expiredAppIds = Sets.newHashSet();
    try {
      for (Map<String, Long> appAndTimes : appAndNums) {
        for (Map.Entry<String, Long> appAndTime : appAndTimes.entrySet()) {
          String appId = appAndTime.getKey();
          long lastReport = appAndTime.getValue();
          appIds.put(appId, lastReport);
          if (System.currentTimeMillis() - lastReport > expired) {
            expiredAppIds.add(appId);
            appAndTimes.remove(appId);
            appIdToUser.remove(appId);
          }
        }
      }
      LOG.info("Start to check status for " + appIds.size() + " applications");
      for (String appId : expiredAppIds) {
        LOG.info("Remove expired application:" + appId);
        appIds.remove(appId);
        if (appIdToRemoteStorageInfo.containsKey(appId)) {
          decRemoteStorageCounter(appIdToRemoteStorageInfo.get(appId).getPath());
          appIdToRemoteStorageInfo.remove(appId);
        }
      }
      CoordinatorMetrics.gaugeRunningAppNum.set(appIds.size());
      updateRemoteStorageMetrics();
    } catch (Exception e) {
      // the flag is only for test case
      hasErrorInStatusCheck = true;
      LOG.warn("Error happened in statusCheck", e);
    }
  }

  private void updateRemoteStorageMetrics() {
    for (String remoteStoragePath : availableRemoteStorageInfo.keySet()) {
      try {
        String storageHost = getStorageHost(remoteStoragePath);
        CoordinatorMetrics.updateDynamicGaugeForRemoteStorage(storageHost,
            remoteStoragePathRankValue.get(remoteStoragePath).getAppNum().get());
      } catch (Exception e) {
        LOG.warn("Update remote storage metrics for {} failed ", remoteStoragePath);
      }
    }
  }

  private void addRemoteStorageMetrics(String remoteStoragePath) {
    String storageHost = getStorageHost(remoteStoragePath);
    if (!StringUtils.isEmpty(storageHost)) {
      CoordinatorMetrics.addDynamicGaugeForRemoteStorage(getStorageHost(remoteStoragePath));
      LOG.info("Add remote storage metrics for {} successfully ", remoteStoragePath);
    }
  }

  private String getStorageHost(String remoteStoragePath) {
    if (remoteStorageToHost.containsKey(remoteStoragePath)) {
      return remoteStorageToHost.get(remoteStoragePath);
    }
    String storageHost = "";
    try {
      URI uri = new URI(remoteStoragePath);
      storageHost = uri.getHost();
      remoteStorageToHost.put(remoteStoragePath, storageHost);
    } catch (URISyntaxException e) {
      LOG.warn("Invalid format of remoteStoragePath to get host, {}", remoteStoragePath);
    }
    return storageHost;
  }

  public Map<String, Integer> getDefaultUserApps() {
    return quotaManager.getDefaultUserApps();
  }

  public QuotaManager getQuotaManager() {
    return quotaManager;
  }

  public static List<String> getPathSchema() {
    return REMOTE_PATH_SCHEMA;
  }

  public enum StrategyName {
    APP_BALANCE,
    IO_SAMPLE
  }
}
