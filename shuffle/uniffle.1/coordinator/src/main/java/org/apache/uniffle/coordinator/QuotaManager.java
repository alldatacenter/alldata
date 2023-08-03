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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.filesystem.HadoopFilesystemProvider;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.common.util.ThreadUtils;

/**
 * QuotaManager is a manager for resource restriction.
 */
public class QuotaManager {
  private static final Logger LOG = LoggerFactory.getLogger(QuotaManager.class);
  private final Map<String, Map<String, Long>> currentUserAndApp = Maps.newConcurrentMap();
  private final Map<String, String> appIdToUser = Maps.newConcurrentMap();
  private final String quotaFilePath;
  private final Integer quotaAppNum;
  private FileSystem hadoopFileSystem;
  private final AtomicLong quotaFileLastModify = new AtomicLong(0L);
  private final Map<String, Integer> defaultUserApps = Maps.newConcurrentMap();

  public QuotaManager(CoordinatorConf conf) {
    this.quotaFilePath = conf.get(CoordinatorConf.COORDINATOR_QUOTA_DEFAULT_PATH);
    this.quotaAppNum = conf.getInteger(CoordinatorConf.COORDINATOR_QUOTA_DEFAULT_APP_NUM);
    if (quotaFilePath == null) {
      LOG.warn("{} is not configured, each user will use the default quota : {}",
          CoordinatorConf.COORDINATOR_QUOTA_DEFAULT_PATH.key(),
          conf.get(CoordinatorConf.COORDINATOR_QUOTA_DEFAULT_APP_NUM));
    } else {
      final Long updateTime = conf.get(CoordinatorConf.COORDINATOR_QUOTA_UPDATE_INTERVAL);
      try {
        hadoopFileSystem = HadoopFilesystemProvider.getFilesystem(new Path(quotaFilePath), new Configuration());
      } catch (Exception e) {
        LOG.error("Cannot init remoteFS on path : " + quotaFilePath, e);
      }
      // Threads that update the number of submitted applications
      ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
          ThreadUtils.getThreadFactory("UpdateDefaultApp-%d"));
      scheduledExecutorService.scheduleAtFixedRate(
          this::detectUserResource, 0, updateTime / 2, TimeUnit.MILLISECONDS);
      LOG.info("QuotaManager initialized successfully.");
    }
  }

  public void detectUserResource() {
    if (hadoopFileSystem != null) {
      try {
        Path hadoopPath = new Path(quotaFilePath);
        FileStatus fileStatus = hadoopFileSystem.getFileStatus(hadoopPath);
        if (fileStatus != null && fileStatus.isFile()) {
          long latestModificationTime = fileStatus.getModificationTime();
          if (quotaFileLastModify.get() != latestModificationTime) {
            parseQuotaFile(hadoopFileSystem.open(hadoopPath));
            LOG.warn("We have updated the file {}.", hadoopPath);
            quotaFileLastModify.set(latestModificationTime);
          }
        }
      } catch (FileNotFoundException fileNotFoundException) {
        LOG.error("Can't find this file {}", quotaFilePath);
      } catch (Exception e) {
        LOG.warn("Error when updating quotaFile, the exclude nodes file path: {}", quotaFilePath);
      }
    }
  }

  public void parseQuotaFile(DataInputStream fsDataInputStream) {
    String content;
    try (BufferedReader bufferedReader =
             new BufferedReader(new InputStreamReader(fsDataInputStream, StandardCharsets.UTF_8))) {
      while ((content = bufferedReader.readLine()) != null) {
        // to avoid reading comments
        if (!content.startsWith("#") && !content.isEmpty()) {
          String user = content.split(Constants.EQUAL_SPLIT_CHAR)[0].trim();
          Integer appNum = Integer.valueOf(content.split(Constants.EQUAL_SPLIT_CHAR)[1].trim());
          defaultUserApps.put(user, appNum);
        }
      }
    } catch (Exception e) {
      LOG.error("Error occur when parsing file {}", quotaFilePath, e);
    }
  }

  public boolean checkQuota(String user, String uuid) {
    Map<String, Long> appAndTimes = currentUserAndApp.computeIfAbsent(user, x -> Maps.newConcurrentMap());
    Integer defaultAppNum = defaultUserApps.computeIfAbsent(user, x -> quotaAppNum);
    synchronized (this) {
      int currentAppNum = appAndTimes.size();
      if (currentAppNum >= defaultAppNum) {
        return true;
      } else {
        appAndTimes.put(uuid, System.currentTimeMillis());
        return false;
      }
    }
  }

  public void registerApplicationInfo(String appId, Map<String, Long> appAndTime) {
    long currentTimeMillis = System.currentTimeMillis();
    String[] appIdAndUuid = appId.split("_");
    String uuidFromApp = appIdAndUuid[appIdAndUuid.length - 1];
    // if appId created successfully, we need to remove the uuid
    synchronized (this) {
      appAndTime.remove(uuidFromApp);
      appAndTime.put(appId, currentTimeMillis);
    }
  }

  @VisibleForTesting
  public Map<String, Integer> getDefaultUserApps() {
    return defaultUserApps;
  }

  public Map<String, Map<String, Long>> getCurrentUserAndApp() {
    return currentUserAndApp;
  }

  public Map<String, String> getAppIdToUser() {
    return appIdToUser;
  }
}
